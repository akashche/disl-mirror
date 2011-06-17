package ch.usi.dag.disl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.analyzer.Analyzer;
import ch.usi.dag.disl.parser.Parser;
import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.snippet.marker.MarkedRegion;
import ch.usi.dag.disl.weaver.Weaver;
import ch.usi.dag.jborat.agent.Instrumentation;

public class DiSLDriver implements Instrumentation {

	final String PROP_DISL_CLASSES = "disl.classes";
	final String PROP_CLASSES_DELIM = ",";
	
	List<Snippet> snippets = new LinkedList<Snippet>();
	List<Analyzer> analyzers = new LinkedList<Analyzer>();
	Weaver weaver;
	
	public DiSLDriver() {
		super();
		
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
		
		Parser parser = new Parser(); 
		
		for(byte [] classAsBytes : compiledClasses) {
			parser.parse(classAsBytes);
		}
		
		// initialize fields
		snippets = parser.getSnippets();
		analyzers = parser.getAnalyzers();
		weaver = new Weaver();
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
			if(snippet.getScope().matches(method)) {
				matchedSnippets.add(snippet);
			}
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
	}
	
	@Override
	public void instrument(ClassNode clazz) {
		
		// instrument all methods in a class
		for(Object methodObj : clazz.methods) {
			
			// cast - ASM still uses Java 1.4 interface
			MethodNode method = (MethodNode) methodObj;
			
			instrumentMethod(clazz, method);
		}
	}

}
