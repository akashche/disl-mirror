package ch.usi.dag.disl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.annotation.parser.AnnotationParser;
import ch.usi.dag.disl.exception.DiSLException;
import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.snippet.marker.MarkedRegion;
import ch.usi.dag.disl.snippet.syntheticlocal.SLVSelector;
import ch.usi.dag.disl.snippet.syntheticlocal.SyntheticLocalVar;
import ch.usi.dag.disl.weaver.Weaver;
import ch.usi.dag.jborat.agent.Instrumentation;

// TODO javadoc comment all
public class DiSL implements Instrumentation {

	final String PROP_DISL_CLASSES = "disl.classes";
	final String PROP_CLASSES_DELIM = ",";
	
	List<Snippet> snippets;
	Map<String, SyntheticLocalVar> syntheticLoclaVars;
	
	public DiSL() {
		super();
		
		// report every exception within our code - don't let anyone mask it
		try {
		
			String classesToCompile = System.getProperty(PROP_DISL_CLASSES);
			
			if(classesToCompile == null) {
				throw new DiSLException(
						"Property " + PROP_DISL_CLASSES + " is not defined");
			}
			
			List<byte []> compiledClasses = new LinkedList<byte []>();
	
			// TODO replace for real compiler
			CompilerStub compiler = new CompilerStub();
			
			// *** compile DiSL classes ***
			for(String file : classesToCompile.split(PROP_CLASSES_DELIM)) {
				
				compiledClasses.add(compiler.compile(file));
			}
			
			// *** parse compiled classes ***
			//  - create snippets
			//  - create analyses
			
			AnnotationParser parser = new AnnotationParser(); 
			
			for(byte [] classAsBytes : compiledClasses) {
				parser.parse(classAsBytes);
			}
			
			// initialize snippets
			snippets = parser.getSnippets();

			// initialize synthetic local variables
			syntheticLoclaVars = parser.getSyntheticLocalVars();
			
			// TODO put checker here
			// like After should catch normal and abnormal execution
			// but if you are using After (AfterThrowing) with BasicBlockMarker
			// or InstructionMarker that doesn't throw exception, then it is
			// probably something, you don't want - so just warn the user
			// also it can warn about unknown opcodes if you let user to
			// specify this for InstructionMarker
		}
		catch(Exception e) {
			reportError(e);
		}
		catch(Throwable e) {
			// unexpected exception, just print stack trace
			e.printStackTrace();
		}
	}

	/**
	 * Instruments a method in a class.
	 * 
	 * NOTE: This method changes the classNode argument
	 * 
	 * @param classNode class that will be instrumented
	 * @param method method in the classNode argument, that will be instrumented
	 */
	private void instrumentMethod(ClassNode classNode, MethodNode method) {
		
		// TODO create finite-state machine if possible
		
		// *** match snippet scope ***
		
		List<Snippet> matchedSnippets = new LinkedList<Snippet>();
		
		for(Snippet snippet : snippets) {
			
			// snippet matching
			if(snippet.getScope().matches(classNode.name, method)) {
				matchedSnippets.add(snippet);
			}
		}
		
		// if there is nothing to instrument -> quit
		// just to be faster out
		if(matchedSnippets.isEmpty()) {
			return;
		}
		
		// *** create markings ***
		
		// all markings in one list for analysis
		List<MarkedRegion> allMarkings = new LinkedList<MarkedRegion>();
		
		// markings according to snippets for viewing
		Map<Snippet, List<MarkedRegion>> snippetMarkings = 
			new HashMap<Snippet, List<MarkedRegion>>();
		
		for(Snippet snippet : matchedSnippets) {
			
			// marking
			List<MarkedRegion> marking = snippet.getMarker().mark(method);
			
			// add to lists
			allMarkings.addAll(marking);
			snippetMarkings.put(snippet, marking);
		}
		
		// *** compute static info ***
		
		// TODO analysis
		// - call analysis for each marked region and create map of computed values
		// - store it in 2 level hash map
		//  - first key is marked region
		//  - second key is id of the invoked method
		
		// *** select synthetic local vars ***

		// we need list of synthetic locals that are actively used in selected
		// (marked) snippets
		
		List<SyntheticLocalVar> selectedSLV = SLVSelector.usedInSnippets(
				snippetMarkings.keySet(), syntheticLoclaVars); 
		
		// *** viewing ***
		
		// TODO ! weaver should have two parts, weaving and rewriting
		Weaver.instrument(classNode, snippetMarkings, selectedSLV);
		
		// TODO just for debugging
		System.out.println("--- instumentation of "
				+ classNode.name + "." + method.name);
	}
	
	public void instrument(ClassNode classNode) {

		// report every exception within our code - don't let anyone mask it
		try {
		
			// instrument all methods in a class
			for(Object methodObj : classNode.methods) {
				
				// cast - ASM still uses Java 1.4 interface
				MethodNode method = (MethodNode) methodObj;
				
				instrumentMethod(classNode, method);
			}
			
		}
		catch(Throwable e) {
			// unexpected exception, just print stack trace
			e.printStackTrace();
		}
	}
	
	private void reportError(Exception e) {

		// error report for user (input) errors
		System.err.println("DiSL error: " + e.getMessage());
		Throwable cause = e.getCause();
		while(cause != null) {
			System.err.println("  Inner error: " + cause.getMessage());
			cause = cause.getCause();
		}
	}
}
