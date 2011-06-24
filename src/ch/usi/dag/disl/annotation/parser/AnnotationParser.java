package ch.usi.dag.disl.annotation.parser;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.analyzer.Analyzer;
import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.AfterAbnormal;
import ch.usi.dag.disl.annotation.AfterNormal;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.annotation.SyntheticLocal;
import ch.usi.dag.disl.exception.DiSLFatalException;
import ch.usi.dag.disl.exception.MarkerException;
import ch.usi.dag.disl.exception.AnnotParserException;
import ch.usi.dag.disl.exception.ScopeParserException;
import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.snippet.SnippetImpl;
import ch.usi.dag.disl.snippet.marker.Marker;
import ch.usi.dag.disl.snippet.marker.MarkerFactory;
import ch.usi.dag.disl.snippet.scope.Scope;
import ch.usi.dag.disl.snippet.scope.ScopeImpl;
import ch.usi.dag.disl.snippet.syntheticlocal.SyntheticLocalVar;
import ch.usi.dag.disl.util.Constants;
import ch.usi.dag.disl.util.InsnListHelper;

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
	
	public void parse(byte[] classAsBytes) 
			throws MarkerException, AnnotParserException, ScopeParserException {

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
		Map<String, SyntheticLocalVar> slVars = 
			parseSyntheticLocalVars(classNode.name, classNode.fields);
		
		// get static initialization code
		InsnList origInitCodeIL = null;
		for(Object methodObj : classNode.methods) {

			// cast - ASM still uses Java 1.4 interface
			MethodNode method = (MethodNode) methodObj;
			
			// get the code
			if (method.name.equals(Constants.STATIC_INIT_NAME)) {
				origInitCodeIL = method.instructions;
				break;
			}
		}
		
		// parse init code for synthetic local vars and assigns them accordingly
		parseInitCodeForSLV(origInitCodeIL, slVars);
		
		// add local vars from this class to others
		syntheticLocalVars.addAll(slVars.values());
		
		for (Object methodObj : classNode.methods) {

			// cast - ASM still uses Java 1.4 interface
			MethodNode method = (MethodNode) methodObj;

			// skip the constructor
			if (method.name.equals(Constants.CONSTRUCTOR_NAME)) {
				continue;
			}
			
			// skip static initializer
			if (method.name.equals(Constants.STATIC_INIT_NAME)) {
				continue;
			}

			snippets.addAll(parseSnippets(classNode.name, method));
		}
	}

	/**
	 * Method creates analyzers according to annotations
	 * 
	 * @param annotations
	 *            contains specifications for analyzers
	 */
	protected List<Analyzer> parseAnalyzers(List<?> annotations) {
		// TODO analysis: implement
		// TODO analysis: check for null
		
		return new LinkedList<Analyzer>();
	}
	
	private Map<String, SyntheticLocalVar> parseSyntheticLocalVars(
			String className, List<?> fields) throws AnnotParserException {
		
		Map<String, SyntheticLocalVar> result = 
			new HashMap<String, SyntheticLocalVar>();
		
		for (Object fieldObj : fields) {
		
			// cast - ASM still uses Java 1.4 interface
			FieldNode field = (FieldNode) fieldObj;
			
			if (field.invisibleAnnotations == null) {
				throw new AnnotParserException("DiSL anottation for field "
						+ field.name + " is missing");
			}
	
			if (field.invisibleAnnotations.size() > 1) {
				throw new AnnotParserException("Field "
						+ field.name + " may have only one anotation");
			}
			
			AnnotationNode annotation = 
				(AnnotationNode) field.invisibleAnnotations.get(0);
			
			Type annotationType = Type.getType(annotation.desc);

			// check annotation type
			if (! annotationType.equals(Type.getType(SyntheticLocal.class))) {
				throw new AnnotParserException("Field " + field.name
						+ " has unsupported DiSL annotation");
			}
			
			// check if field is static
			if((field.access & Opcodes.ACC_STATIC) == 0) {
				throw new AnnotParserException("Field " + field.name
						+ " declared as SyntheticLocal but is not static");
			}

			// add to results
			SyntheticLocalVar slv =
				new SyntheticLocalVar(className, field.name);
			result.put(slv.getID(), slv);
		}
		
		return result;
	}
	
	private void parseInitCodeForSLV(InsnList origInitCodeIL,
			Map<String, SyntheticLocalVar> slVars) {
		
		// first initialization instruction for some field
		AbstractInsnNode firstInitInsn = origInitCodeIL.getFirst();
		
		for(AbstractInsnNode instr : origInitCodeIL.toArray()) {

			// if our instruction is field
			if (instr instanceof FieldInsnNode) {
				
				FieldInsnNode fieldInstr = (FieldInsnNode) instr;

				// get whole name of the field
				String wholeFieldName = fieldInstr.owner
						+ SyntheticLocalVar.NAME_DELIM + fieldInstr.name;
				
				SyntheticLocalVar slv = slVars.get(wholeFieldName);
				
				if(slv == null) {
					throw new DiSLFatalException(
							"Initialization of static field " + wholeFieldName
							+ " found, but no such field declared");
				}
				
				// clone part of the asm code
				InsnList initASMCode = InsnListHelper.cloneList(origInitCodeIL,
						firstInitInsn, instr);
				
				// store the code
				slv.setInitASMCode(initASMCode);
				
				// prepare first init for next field
				firstInitInsn = instr.getNext();
			}
		
			// if opcode is return then we are done
			if(InsnListHelper.isReturn(instr.getOpcode())) {
				break;
			}
		}
	}

	protected List<Snippet> parseSnippets(String className, MethodNode method)
			throws MarkerException, AnnotParserException, ScopeParserException {

		List<Snippet> result = new LinkedList<Snippet>();
		
		if (method.invisibleAnnotations == null) {
			throw new AnnotParserException("DiSL anottation for method "
					+ method.name + " is missing");
		}

		// more annotations on one snippet
		// supported but we will have multiple snippets ;)
		for (Object annotationObj : method.invisibleAnnotations) {

			MethodAnnotationData annotData =
			// cast - ASM still uses Java 1.4 interface
			parseMethodAnnotation((AnnotationNode) annotationObj);

			// if this is unknown annotation
			if (! annotData.isKnown()) {
				throw new AnnotParserException("Method " + method.name
						+ " has unsupported DiSL annotation");
			}

			// marker
			Marker marker = MarkerFactory.createMarker(annotData.getMarker());

			// scope
			Scope scope = new ScopeImpl(annotData.getScope());

			// process code
			SnippetCodeData scd = 
				processSnippetCode(className, method.instructions); 
			
			// whole snippet
			result.add(new SnippetImpl(annotData.getType(), marker, scope,
					annotData.getOrder(), scd.getAsmCode(),
					scd.getReferencedSLV()));
		}
		
		return result;
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
	
	protected MethodAnnotationData parseMethodAnnotation(
			AnnotationNode annotation) {

		Type annotationType = Type.getType(annotation.desc);

		// after annotation
		if (annotationType.equals(Type.getType(After.class))) {
			return parseMethodAnnotFields(After.class, annotation,
					annotation.values);
		}
		
		// after normal execution annotation
		if (annotationType.equals(Type.getType(AfterNormal.class))) {
			return parseMethodAnnotFields(AfterNormal.class, annotation,
					annotation.values);
		}
		
		// after abnormal execution annotation
		if (annotationType.equals(Type.getType(AfterAbnormal.class))) {
			return parseMethodAnnotFields(AfterAbnormal.class, annotation,
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

			throw new DiSLFatalException("Unknow field " + name
					+ " in annotation " + type.toString()
					+ ". This may happen if annotation class is changed but"
					+ " parser is not.");
		}

		if (marker == null || scope == null || order == null) {

			throw new DiSLFatalException("Missing field in annotation "
					+ type.toString()
					+ ". This may happen if annotation class is changed but"
					+ " parser is not.");
		}

		return new MethodAnnotationData(type, marker, scope, order);
	}
	
	private class SnippetCodeData {
		
		private InsnList asmCode;
		private Set<String> referencedSLV;
		
		public SnippetCodeData(InsnList asmCode, Set<String> referencedSLV) {
			super();
			this.asmCode = asmCode;
			this.referencedSLV = referencedSLV;
		}

		public InsnList getAsmCode() {
			return asmCode;
		}

		public Set<String> getReferencedSLV() {
			return referencedSLV;
		}
	}
	
	private SnippetCodeData processSnippetCode(String className,
			InsnList snippetCode) {

		// clone the instrucition list
		InsnList asmCode = InsnListHelper.cloneList(snippetCode);
		
		// detect empty stippets
		if(InsnListHelper.containsOnlyReturn(asmCode)) {
			return new SnippetCodeData(null, null);
		}
		
		// remove returns in snippet (in asm code)
		InsnListHelper.removeReturns(asmCode);
		
		Set<String> slvList = new HashSet<String>();
		
		// create list of synthetic local variables
		for(AbstractInsnNode instr : asmCode.toArray()) {

			// if our instruction is field
			if (instr instanceof FieldInsnNode) {
				
				FieldInsnNode fieldInstr = (FieldInsnNode) instr;

				// we've found SyntheticLocal variable
				if(className.equals(fieldInstr.owner)) {
					
					// get whole name of the field
					String wholeFieldName = fieldInstr.owner
							+ SyntheticLocalVar.NAME_DELIM + fieldInstr.name;
					
					slvList.add(wholeFieldName);
				}
			}
		}

		return new SnippetCodeData(asmCode, slvList);
	}
}
