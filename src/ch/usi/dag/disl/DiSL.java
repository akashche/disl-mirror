package ch.usi.dag.disl;

import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.dislclass.loader.ClassByteLoader;
import ch.usi.dag.disl.dislclass.localvar.SyntheticLocalVar;
import ch.usi.dag.disl.dislclass.parser.ClassParser;
import ch.usi.dag.disl.dislclass.processor.Proc;
import ch.usi.dag.disl.dislclass.snippet.Snippet;
import ch.usi.dag.disl.dislclass.snippet.marker.MarkedRegion;
import ch.usi.dag.disl.exception.DiSLFatalException;
import ch.usi.dag.disl.exception.InitException;
import ch.usi.dag.disl.exception.ProcessorException;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticInfoException;
import ch.usi.dag.disl.processor.generator.PIResolver;
import ch.usi.dag.disl.processor.generator.ProcGenerator;
import ch.usi.dag.disl.processor.generator.ProcInstance;
import ch.usi.dag.disl.processor.generator.ProcMethodInstance;
import ch.usi.dag.disl.staticinfo.StaticInfo;
import ch.usi.dag.disl.weaver.Weaver;
import ch.usi.dag.jborat.agent.Instrumentation;

// TODO javadoc comment all
public class DiSL implements Instrumentation {

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

			String useDynBypassStr = System.getProperty(PROP_DYNAMIC_BYPASS);

			boolean useDynamicBypass = false;
			if (useDynBypassStr != null) {
				useDynamicBypass = useDynBypassStr.toLowerCase().equals(
						PROP_DYNAMIC_BYPASS_TRUE);
			}

			List<InputStream> dislClasses = ClassByteLoader.loadDiSLClasses();
			
			if(dislClasses == null) {
				throw new InitException("Cannot load DiSL classes. Please set" +
						" the property " + ClassByteLoader.PROP_DISL_CLASSES
						+ " or supply jar with DiSL classes"
						+ " and proper manifest");
			}

			// *** parse disl classes ***
			// - create snippets
			// - create static analysis methods

			ClassParser parser = new ClassParser();

			for (InputStream classIS : dislClasses) {
				parser.parse(classIS);
			}

			// initialize processors
			Map<Type, Proc> processors = parser.getProcessors();
			for (Proc processor : processors.values()) {
				processor.init(parser.getAllLocalVars());
			}

			List<Snippet> parsedSnippets = parser.getSnippets();

			// initialize snippets
			for (Snippet snippet : parsedSnippets) {
				snippet.init(parser.getAllLocalVars(), processors,
						useDynamicBypass);
			}
			
			// initialize snippets variable
			//  - this is set when everything is ok
			//  - it serves as initialization flag
			snippets = parsedSnippets;

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
			throws ReflectionException, StaticInfoException,
			ProcessorException {

		// skip abstract methods
		if ((methodNode.access & Opcodes.ACC_ABSTRACT) != 0) {
			return;
		}
		
		// skip native methods
		if ((methodNode.access & Opcodes.ACC_NATIVE) != 0) {
			return;
		}
		
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

		// *** prepare processors ***

		String fullMethodName = classNode.name + "." + methodNode.name;

		PIResolver piResolver = new ProcGenerator().compute(fullMethodName,
				methodNode, snippetMarkings);

		// *** used synthetic local vars in processors ***
		
		// include SLVs from processor methods into usedSLV
		for(ProcInstance pi : piResolver.getAllProcInstances()) {
			
			for(ProcMethodInstance pmi : pi.getMethods()) {
				
				usedSLVs.addAll(pmi.getCode().getReferencedSLVs());
			}
		}

		// *** viewing ***

		Weaver.instrument(classNode, methodNode, snippetMarkings,
				new LinkedList<SyntheticLocalVar>(usedSLVs), staticInfo,
				piResolver);

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
