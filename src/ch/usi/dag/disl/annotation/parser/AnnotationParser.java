package ch.usi.dag.disl.annotation.parser;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.analyzer.Analyzer;
import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.snippet.SnippetImpl;
import ch.usi.dag.disl.snippet.marker.Marker;
import ch.usi.dag.disl.snippet.marker.MarkerFactory;
import ch.usi.dag.disl.snippet.scope.Scope;
import ch.usi.dag.disl.snippet.scope.ScopeImpl;
import ch.usi.dag.disl.snippet.syntheticlocal.SyntheticLocalVar;
import ch.usi.dag.disl.util.Constants;

/**
 * The parser takes annotated java file as input and creates Snippet and
 * Analyzer classes
 */
public class AnnotationParser {

	private List<Snippet> snippets = new LinkedList<Snippet>();
	private List<Analyzer> analyzers = new LinkedList<Analyzer>();
	private List<SyntheticLocalVar> syntheticLocalVars = 
		new LinkedList<SyntheticLocalVar>();

	public List<Snippet> getSnippets() {

		return snippets;
	}

	public List<Analyzer> getAnalyzers() {

		return analyzers;
	}

	public List<SyntheticLocalVar> getSyntheticLocalVars() {

		return syntheticLocalVars;
	}
	
	public void parse(byte[] classAsBytes) {

		// NOTE this method can be called many times

		ClassReader cr = new ClassReader(classAsBytes);
		ClassNode classNode = new ClassNode();
		cr.accept(classNode, 0);

		analyzers.addAll(parseAnalyzers(classNode.invisibleAnnotations));
		
		// support for synthetic local
		// - if two synthetic local vars with the same name are defined
		//   in different files they will be prefixed with class name as it is
		//   also in byte code
		
		// parse annotations
		List<SyntheticLocalVar> slVars = 
			parseSyntheticLocalVars(classNode.fields);
		
		// get static initialization code
		InsnList origInitCodeIL = null;
		for(Object methodObj : classNode.methods) {

			// cast - ASM still uses Java 1.4 interface
			MethodNode method = (MethodNode) methodObj;
			
			// get the code
			if (method.name.equals(Constants.CONSTRUCTOR_NAME)) {
				origInitCodeIL = method.instructions;
				break;
			}
		}
		
		// parse init code for synthetic local vars and assigns them accordingly
		parseInitCodeForSLV(origInitCodeIL, slVars);
		
		// add them into the list
		syntheticLocalVars.addAll(slVars);
		
		for (Object methodObj : classNode.methods) {

			// cast - ASM still uses Java 1.4 interface
			MethodNode method = (MethodNode) methodObj;

			// skip the constructor
			if (method.name.equals(Constants.CONSTRUCTOR_NAME)) {
				continue;
			}

			snippets.addAll(parseSnippets(method));
		}
	}

	/**
	 * Method creates analyzers according to annotations
	 * 
	 * @param annotations
	 *            contains specifications for analyzers
	 */
	protected List<Analyzer> parseAnalyzers(List<?> annotations) {
		// TODO implement
		// TODO check for null
		
		return new LinkedList<Analyzer>();
	}
	
	private List<SyntheticLocalVar> parseSyntheticLocalVars(List<?> fields) {
		// TODO !
		return null;
	}
	
	private void parseInitCodeForSLV(InsnList origInitCodeIL,
			List<SyntheticLocalVar> slVars) {
		// TODO !
		
	}

	// data holder for parseMethodAnnotation methods
	private class MethodAnnotationData {

		private boolean known;
		private Class<?> type;
		private Type marker;
		private String scope;
		private int order;

		public MethodAnnotationData() {
			this.known = false;
		}

		public MethodAnnotationData(Class<?> type, Type marker, String scope,
				int order) {
			super();

			this.known = true;
			this.type = type;
			this.marker = marker;
			this.scope = scope;
			this.order = order;
		}

		public boolean isKnown() {
			return known;
		}

		public Class<?> getType() {
			return type;
		}

		public Type getMarker() {
			return marker;
		}

		public String getScope() {
			return scope;
		}

		public int getOrder() {
			return order;
		}
	}

	protected List<Snippet> parseSnippets(MethodNode method) {

		List<Snippet> result = new LinkedList<Snippet>();
		
		if (method.invisibleAnnotations == null) {
			// TODO report user errors
			throw new RuntimeException("Method " + method.name
					+ " has no anottations which is unsupported");
		}

		// more annotations on one snippet
		// supported but we will have multiple snippets ;)
		for (Object annotationObj : method.invisibleAnnotations) {

			MethodAnnotationData annotData =
			// cast - ASM still uses Java 1.4 interface
			parseMethodAnnotation((AnnotationNode) annotationObj);

			// if this is unknown annotation skip it
			if (!annotData.isKnown()) {
				continue;
			}

			// marker
			Marker marker = MarkerFactory.createMarker(annotData.getMarker());

			// scope
			Scope scope = new ScopeImpl(annotData.getScope());

			// whole snippet
			result.add(new SnippetImpl(annotData.getType(), marker, scope,
					annotData.getOrder(), method.instructions));
		}
		
		return result;
	}

	protected MethodAnnotationData parseMethodAnnotation(
			AnnotationNode annotation) {

		Type annotationType = Type.getType(annotation.desc);

		// after annotation
		if (annotationType.equals(Type.getType(After.class))) {
			return parseMethodAnnotFields(After.class, annotation,
					annotation.values);
		}

		// before annotation
		if (annotationType.equals(Type.getType(Before.class))) {
			return parseMethodAnnotFields(Before.class, annotation,
					annotation.values);
		}

		// unknown annotation
		return new MethodAnnotationData();
	}

	private MethodAnnotationData parseMethodAnnotFields(Class<?> type,
			AnnotationNode annotation, List<?> annotValues) {

		Iterator<?> it = annotValues.iterator();

		String name = null;
		Type marker = null;
		String scope = null;
		Integer order = null;

		while (it.hasNext()) {

			name = (String) it.next();

			if (name.equals("marker")) {

				marker = (Type) it.next();
				continue;
			}

			if (name.equals("scope")) {

				scope = (String) it.next();
				continue;
			}

			if (name.equals("order")) {

				order = (Integer) it.next();
				continue;
			}

			throw new RuntimeException("Unknow field " + name
					+ " in annotation " + type.toString()
					+ ". This may happen if annotation class is changed but"
					+ " parser is not.");
		}

		if (marker == null || scope == null || order == null) {

			throw new RuntimeException("Missing field in annotation "
					+ type.toString()
					+ ". This may happen if annotation class is changed but"
					+ " parser is not.");
		}

		return new MethodAnnotationData(type, marker, scope, order);
	}
}
