package ch.usi.dag.disl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.exception.ASMException;
import ch.usi.dag.disl.exception.DiSLFatalException;
import ch.usi.dag.disl.exception.InitException;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticAnalysisException;
import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.snippet.localvars.SyntheticLocalVar;
import ch.usi.dag.disl.snippet.localvars.ThreadLocalVar;
import ch.usi.dag.disl.snippet.marker.MarkedRegion;
import ch.usi.dag.disl.snippet.parser.SnippetParser;
import ch.usi.dag.disl.staticinfo.StaticInfo;
import ch.usi.dag.disl.weaver.Weaver;
import ch.usi.dag.jborat.agent.Instrumentation;

// TODO javadoc comment all
public class DiSL implements Instrumentation {

	final String PROP_DISL_CLASSES = "disl.classes";
	final String PROP_CLASSES_DELIM = ",";

	final String PROP_DYNAMIC_BYPASS = "disl.dynbypass";
	final String PROP_DYNAMIC_BYPASS_TRUE = "yes";

	List<Snippet> snippets;

	// list of static analysis instances
	// validity of an instance is for one instrumented class
	// instances are created lazily when needed in StaticInfo
	Map<Class<?>, Object> staticAnalysisInstances;

	public void initialize() {

		// report every exception within our code - don't let anyone mask it
		try {

			String classesToCompile = System.getProperty(PROP_DISL_CLASSES);

			if (classesToCompile == null) {
				throw new InitException("Property " + PROP_DISL_CLASSES
						+ " is not defined");
			}

			String useDynBypassStr = System.getProperty(PROP_DYNAMIC_BYPASS);

			boolean useDynamicBypass = false;
			if (useDynBypassStr != null) {
				useDynamicBypass = useDynBypassStr.toLowerCase().equals(
						PROP_DYNAMIC_BYPASS_TRUE);
			}

			List<byte[]> compiledClasses = new LinkedList<byte[]>();

			// TODO replace for real compiler
			CompilerStub compiler = new CompilerStub();

			// *** compile DiSL classes ***
			for (String file : classesToCompile.split(PROP_CLASSES_DELIM)) {

				compiledClasses.add(compiler.compile(file));
			}

			// *** parse compiled classes ***
			// - create snippets
			// - create static analysis methods

			SnippetParser parser = new SnippetParser();

			for (byte[] classAsBytes : compiledClasses) {
				parser.parse(classAsBytes);
			}

			// initialize snippets
			snippets = parser.getSnippets();

			for (Snippet snippet : snippets) {
				snippet.prepare(parser.getAllLocalVars(), useDynamicBypass);
			}

			// TODO put checker here
			// like After should catch normal and abnormal execution
			// but if you are using After (AfterThrowing) with BasicBlockMarker
			// or InstructionMarker that doesn't throw exception, then it is
			// probably something, you don't want - so just warn the user
			// also it can warn about unknown opcodes if you let user to
			// specify this for InstructionMarker
		} catch (Exception e) {
			reportError(e);
			// TODO just for debugging
			e.printStackTrace();
		} catch (Throwable e) {
			// unexpected exception, just print stack trace
			e.printStackTrace();
		}
	}

	/**
	 * Instruments a method in a class.
	 * 
	 * NOTE: This method changes the classNode argument
	 * 
	 * @param classNode
	 *            class that will be instrumented
	 * @param methodNode
	 *            method in the classNode argument, that will be instrumented
	 * @throws StaticAnalysisException
	 * @throws ReflectionException
	 * @throws ASMException
	 */
	private void instrumentMethod(ClassNode classNode, MethodNode methodNode)
			throws ReflectionException, StaticAnalysisException, ASMException {

		// TODO create finite-state machine if possible

		// *** match snippet scope ***

		List<Snippet> matchedSnippets = new LinkedList<Snippet>();

		for (Snippet snippet : snippets) {

			// snippet matching
			if (snippet.getScope().matches(classNode.name, methodNode)) {
				matchedSnippets.add(snippet);
			}
		}

		// if there is nothing to instrument -> quit
		// just to be faster out
		if (matchedSnippets.isEmpty()) {
			return;
		}

		// *** create markings ***

		// all markings in one list for static analysis
		List<MarkedRegion> allMarkings = new LinkedList<MarkedRegion>();

		// markings according to snippets for viewing
		Map<Snippet, List<MarkedRegion>> snippetMarkings = new HashMap<Snippet, List<MarkedRegion>>();

		for (Snippet snippet : matchedSnippets) {

			// marking
			List<MarkedRegion> marking = snippet.getMarker().mark(methodNode);

			// add to lists
			allMarkings.addAll(marking);
			snippetMarkings.put(snippet, marking);
		}

		// *** compute static info ***

		// prepares StaticInfo class (computes static analysis)
		StaticInfo staticInfo = new StaticInfo(staticAnalysisInstances,
				classNode, methodNode, snippetMarkings);

		// *** select synthetic local vars ***

		// we need list of synthetic and thread locals that are actively used in
		// selected (marked) snippets

		Set<SyntheticLocalVar> selectedSLV = new HashSet<SyntheticLocalVar>();
		Set<ThreadLocalVar> selectedTLV = new HashSet<ThreadLocalVar>();
		for (Snippet snippet : snippetMarkings.keySet()) {
			selectedSLV.addAll(snippet.getCode().getReferencedSLV());
			selectedTLV.addAll(snippet.getCode().getReferencedTLV());
		}

		// *** determine if any snippet uses dynamic analysis ***

		boolean usesDynamicAnalysis = false;
		for (Snippet snippet : snippetMarkings.keySet()) {

			if (snippet.getCode().usesDynamicAnalysis()) {
				usesDynamicAnalysis = true;
				break;
			}
		}

		// *** viewing ***

		// TODO ! weaver should have two parts, weaving and rewriting
		Weaver.instrument(classNode, methodNode, snippetMarkings,
				new LinkedList<SyntheticLocalVar>(selectedSLV),
				new LinkedList<ThreadLocalVar>(selectedTLV),
				staticInfo, usesDynamicAnalysis);

		// TODO ProcessorHack remove
		ProcessorHack.instrument(classNode, methodNode,
				new LinkedList<SyntheticLocalVar>(selectedSLV));

		// TODO just for debugging
		System.out.println("--- instumentation of " + classNode.name + "."
				+ methodNode.name);
	}

	public void instrument(ClassNode classNode) {

		if (snippets == null) {
			throw new DiSLFatalException("DiSL was not initialized");
		}

		// report every exception within our code - don't let anyone mask it
		try {

			// list of static analysis instances
			// validity of an instance is for one instrumented class
			staticAnalysisInstances = new HashMap<Class<?>, Object>();

			// instrument all methods in a class
			for (MethodNode methodNode : classNode.methods) {

				instrumentMethod(classNode, methodNode);
			}

		} catch (Throwable e) {
			// unexpected exception, just print stack trace
			e.printStackTrace();
		}
	}

	private void reportError(Exception e) {

		// error report for user (input) errors
		System.err.println("DiSL error: " + e.getMessage());
		Throwable cause = e.getCause();
		while (cause != null) {
			System.err.println("  Inner error: " + cause.getMessage());
			cause = cause.getCause();
		}
	}
}
