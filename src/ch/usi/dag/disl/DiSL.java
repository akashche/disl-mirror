package ch.usi.dag.disl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.analyzer.Analyzer;
import ch.usi.dag.disl.annotation.parser.AnnotationParser;
import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.snippet.marker.MarkedRegion;
import ch.usi.dag.disl.weaver.Weaver;
import ch.usi.dag.jborat.agent.Instrumentation;

public class DiSL implements Instrumentation {

	final String PROP_DISL_CLASSES = "disl.classes";
	final String PROP_CLASSES_DELIM = ",";
	
	List<Snippet> snippets = new LinkedList<Snippet>();
	List<Analyzer> analyzers = new LinkedList<Analyzer>();
	Weaver weaver;
	
	public DiSL() {
		super();
		
		// report every exception within our code - dont let anyone mask it
		try {
		
			String classesToCompile = System.getProperty(PROP_DISL_CLASSES);
			List<byte []> compiledClasses = new LinkedList<byte []>();
	
			// TODO replace for real compiler
			CompilerStub compiler = new CompilerStub();
			
			// *** compile DiSL classes ***
			for(String file : classesToCompile.split(PROP_CLASSES_DELIM)) {
				
				compiledClasses.add(compiler.compile(file));
			}
			
			// *** parse compiled classes ***
			//  - create snippets
			//  - create analyzers
			
			AnnotationParser parser = new AnnotationParser(); 
			
			for(byte [] classAsBytes : compiledClasses) {
				parser.parse(classAsBytes);
			}
			
			// TODO support for synthetic local
			
			// initialize fields
			snippets = parser.getSnippets();
			analyzers = parser.getAnalyzers();
			weaver = new Weaver();
		}
		catch(Throwable e) {
			reportError(e);
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
		
		// *** analyze ***
		
		// TODO think about structure for analysis
		//  - what all we need to analyze and what (structure) is the output
		
		// *** viewing ***
		
		weaver.instrument(classNode, snippetMarkings);
		
		// TODO debug --
		System.out.println("--- instumentation of " + classNode.name);
		//TraceClassVisitor tcv = new TraceClassVisitor(new PrintWriter(System.out));
		//classNode.accept(tcv);
		System.out.println("---");
	}
	
	public void instrument(ClassNode classNode) {

		// report every exception within our code - dont let anyone mask it
		try {
		
			// instrument all methods in a class
			for(Object methodObj : classNode.methods) {
				
				// cast - ASM still uses Java 1.4 interface
				MethodNode method = (MethodNode) methodObj;
				
				instrumentMethod(classNode, method);
			}
			
		}
		catch(Throwable e) {
			reportError(e);
		}
	}
	
	private void reportError(Throwable e) {

		// TODO something better
		e.printStackTrace();
	}
}