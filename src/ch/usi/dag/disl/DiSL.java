package ch.usi.dag.disl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.dislclass.localvar.SyntheticLocalVar;
import ch.usi.dag.disl.dislclass.parser.ClassParser;
import ch.usi.dag.disl.dislclass.processor.Proc;
import ch.usi.dag.disl.dislclass.snippet.ProcInvocation;
import ch.usi.dag.disl.dislclass.snippet.Snippet;
import ch.usi.dag.disl.dislclass.snippet.marker.MarkedRegion;
import ch.usi.dag.disl.exception.DiSLFatalException;
import ch.usi.dag.disl.exception.InitException;
import ch.usi.dag.disl.exception.ProcessorException;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticAnalysisException;
import ch.usi.dag.disl.processor.ProcessorApplyType;
import ch.usi.dag.disl.processor.generator.PIResolver;
import ch.usi.dag.disl.processor.generator.ProcGenerator;
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

			// TODO replace with jar support
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

			// TODO replace with jar support
			CompilerStub compiler = new CompilerStub();

			// *** compile DiSL classes ***
			for (String file : classesToCompile.split(PROP_CLASSES_DELIM)) {

				compiledClasses.add(compiler.compile(file));
			}

			// *** parse compiled classes ***
			// - create snippets
			// - create static analysis methods

			ClassParser parser = new ClassParser();

			for (byte[] classAsBytes : compiledClasses) {
				parser.parse(classAsBytes);
			}

			// initialize processors
			Map<Class<?>, Proc> processors = parser.getProcessors();
			for (Proc processor : processors.values()) {
				processor.init(parser.getAllLocalVars());
			}

			// initialize snippets
			snippets = parser.getSnippets();

			for (Snippet snippet : snippets) {
				snippet.init(parser.getAllLocalVars(), processors,
						useDynamicBypass);
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
	 */
	private void instrumentMethod(ClassNode classNode, MethodNode methodNode)
			throws ReflectionException, StaticAnalysisException,
			ProcessorException {

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

		// *** used synthetic local vars in snippets ***
		// weaver needs list of synthetic locals that are actively used in
		// selected (matched) snippets

		Set<SyntheticLocalVar> usedSLVs = new HashSet<SyntheticLocalVar>();
		for (Snippet snippet : snippetMarkings.keySet()) {
			usedSLVs.addAll(snippet.getCode().getReferencedSLVs());
		}

		// *** determine if any snippet uses dynamic analysis
		// or any snippet contains processor that has to access stack ***
		// this determines if weaver should do stack computation

		boolean usesDynamicAnalysis = false;
		for (Snippet snippet : snippetMarkings.keySet()) {

			// snipet uses dynamic analysis
			if (snippet.getCode().usesDynamicAnalysis()) {
				usesDynamicAnalysis = true;
				break;
			}

			// processor has to access stack
			for (ProcInvocation prcInv : snippet.getCode()
					.getInvokedProcessors().values()) {

				if (prcInv.getProcApplyType() == ProcessorApplyType.BEFORE_INVOCATION) {
					usesDynamicAnalysis = true;
					break;
				}
			}
		}

		// *** prepare processors ***

		String fullMethodName = classNode.name + "." + methodNode.name;

		PIResolver piResolver = new ProcGenerator().compute(fullMethodName,
				methodNode, snippetMarkings);

		// *** used synthetic local vars in processors ***
		// TODO ! processor - include SLVs from processors into usedSLV

		// *** viewing ***

		// TODO ! weaver should have two parts, weaving and rewriting
		Weaver.instrument(classNode, methodNode, snippetMarkings,
				new LinkedList<SyntheticLocalVar>(usedSLVs), staticInfo,
				usesDynamicAnalysis, piResolver);

		// TODO ! ProcessorHack remove
		ProcessorHack.instrument(classNode, methodNode,
				new LinkedList<SyntheticLocalVar>(usedSLVs));

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
