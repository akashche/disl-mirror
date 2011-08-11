package ch.usi.dag.disl.snippet.parser;

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
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.ProcessorHack;
import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.AfterReturning;
import ch.usi.dag.disl.annotation.AfterThrowing;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.annotation.SyntheticLocal;
import ch.usi.dag.disl.dynamicinfo.DynamicContext;
import ch.usi.dag.disl.exception.DiSLFatalException;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.ScopeParserException;
import ch.usi.dag.disl.exception.SnippetParserException;
import ch.usi.dag.disl.exception.StaticAnalysisException;
import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.snippet.UnprocessedSnippetCode;
import ch.usi.dag.disl.snippet.localvars.LocalVars;
import ch.usi.dag.disl.snippet.localvars.SyntheticLocalVar;
import ch.usi.dag.disl.snippet.localvars.ThreadLocalVar;
import ch.usi.dag.disl.snippet.marker.Marker;
import ch.usi.dag.disl.snippet.scope.Scope;
import ch.usi.dag.disl.snippet.scope.ScopeImpl;
import ch.usi.dag.disl.staticinfo.analysis.StaticAnalysis;
import ch.usi.dag.disl.util.AsmHelper;
import ch.usi.dag.disl.util.Constants;
import ch.usi.dag.disl.util.Parameter;
import ch.usi.dag.disl.util.ReflectionHelper;

/**
 * The parser takes annotated java file as input and creates Snippet classes
 */
public class SnippetParser {

	private List<Snippet> snippets = new LinkedList<Snippet>();

	private LocalVars allLocalVars = new LocalVars();

	public LocalVars getAllLocalVars() {
		return allLocalVars;
	}

	public List<Snippet> getSnippets() {

		return snippets;
	}

	public void parse(byte[] classAsBytes) throws SnippetParserException,
			ReflectionException, ScopeParserException, StaticAnalysisException {

		// NOTE this method can be called many times

		ClassReader cr = new ClassReader(classAsBytes);
		ClassNode classNode = new ClassNode();
		cr.accept(classNode, 0);

		// support for synthetic local
		// - if two synthetic local vars with the same name are defined
		// in different files they will be prefixed with class name as it is
		// also in byte code

		// parse annotations
		LocalVars localVars = parseLocalVars(classNode.name, classNode.fields);

		// add local vars from this class to all local vars from all classes
		allLocalVars.putAll(localVars);

		// get static initialization code
		InsnList origInitCodeIL = null;
		for (MethodNode method : classNode.methods) {

			// get the code
			if (method.name.equals(Constants.STATIC_INIT_NAME)) {
				origInitCodeIL = method.instructions;
				break;
			}
		}

		// parse init code for synthetic local vars and assigns them accordingly
		if (origInitCodeIL != null) {
			parseInitCodeForSLV(origInitCodeIL, localVars.getSyntheticLocals());
		}

		for (MethodNode method : classNode.methods) {

			// skip the constructor
			if (method.name.equals(Constants.CONSTRUCTOR_NAME)) {
				continue;
			}

			// skip static initializer
			if (method.name.equals(Constants.STATIC_INIT_NAME)) {
				continue;
			}

			// TODO ProcessorHack remove
			if (method.name.startsWith("processor")) {

				ProcessorHack.parseProcessor(classNode.name, method);
				continue;
			}

			snippets.add(parseSnippets(classNode.name, method));
		}
	}

	private LocalVars parseLocalVars(String className, List<FieldNode> fields)
			throws SnippetParserException {

		LocalVars result = new LocalVars();

		for (FieldNode field : fields) {

			if (field.invisibleAnnotations == null) {
				throw new SnippetParserException("DiSL annotation for field "
						+ field.name + " is missing");
			}

			if (field.invisibleAnnotations.size() > 1) {
				throw new SnippetParserException("Field " + field.name
						+ " may have only one anotation");
			}

			AnnotationNode annotation = (AnnotationNode) field.invisibleAnnotations
					.get(0);

			Type annotationType = Type.getType(annotation.desc);

			// thread local
			if (annotationType.equals(Type
					.getType(ch.usi.dag.disl.annotation.ThreadLocal.class))) {

				ThreadLocalVar tlv = parseThreadLocal(className, field,
						annotation);

				result.getThreadLocals().put(tlv.getID(), tlv);

				continue;
			}

			// synthetic local
			if (annotationType.equals(Type.getType(SyntheticLocal.class))) {

				SyntheticLocalVar slv = parseSyntheticLocal(className, field,
						annotation);

				result.getSyntheticLocals().put(slv.getID(), slv);

				continue;
			}

			throw new SnippetParserException("Field " + field.name
					+ " has unsupported DiSL annotation");
		}

		return result;
	}

	private ThreadLocalVar parseThreadLocal(String className, FieldNode field,
			AnnotationNode annotation) throws SnippetParserException {

		// check if field is static
		if ((field.access & Opcodes.ACC_STATIC) == 0) {
			throw new SnippetParserException("Field " + field.name
					+ " declared as ThreadLocal but is not static");
		}

		// here we can ignore annotation parsing - already done by start utility
		return new ThreadLocalVar(className, field.name);
	}

	private SyntheticLocalVar parseSyntheticLocal(String className,
			FieldNode field, AnnotationNode annotation)
			throws SnippetParserException {

		// check if field is static
		if ((field.access & Opcodes.ACC_STATIC) == 0) {
			throw new SnippetParserException("Field " + field.name
					+ " declared as SyntheticLocal but is not static");
		}

		// default val for init
		SyntheticLocal.Initialize slvInit = SyntheticLocal.Initialize.ALWAYS;

		if (annotation.values != null) {

			Iterator<?> it = annotation.values.iterator();

			while (it.hasNext()) {

				String name = (String) it.next();

				if (name.equals("initialize")) {

					// parse enum from string
					// first is class and second is enum value
					String[] slvInitStr = (String[]) it.next();
					slvInit = SyntheticLocal.Initialize.valueOf(slvInitStr[1]);

					continue;
				}

				throw new DiSLFatalException("Unknow field " + name
						+ " in annotation at " + field.name
						+ ". This may happen if annotation class is changed"
						+ " but parser is not.");
			}
		}

		return new SyntheticLocalVar(className, field.name, slvInit);
	}

	private InsnList simpleInsnListClone(InsnList src, AbstractInsnNode from,
			AbstractInsnNode to) {

		// empty map - we should not encounter labels here
		Map<LabelNode, LabelNode> map = new HashMap<LabelNode, LabelNode>();

		InsnList dst = new InsnList();

		// copy instructions using clone
		AbstractInsnNode instr = from;
		while (instr != to.getNext()) {

			dst.add(instr.clone(map));

			instr = instr.getNext();
		}

		return dst;
	}

	// synthetic local var initialization can contain only basic constants
	// or single method calls
	private void parseInitCodeForSLV(InsnList origInitCodeIL,
			Map<String, SyntheticLocalVar> slVars) {

		// first initialization instruction for some field
		AbstractInsnNode firstInitInsn = origInitCodeIL.getFirst();

		for (AbstractInsnNode instr : origInitCodeIL.toArray()) {

			// if our instruction is field
			if (instr instanceof FieldInsnNode) {

				FieldInsnNode fieldInstr = (FieldInsnNode) instr;

				// get whole name of the field
				String wholeFieldName = fieldInstr.owner
						+ SyntheticLocalVar.NAME_DELIM + fieldInstr.name;

				SyntheticLocalVar slv = slVars.get(wholeFieldName);

				if (slv == null) {
					throw new DiSLFatalException(
							"Initialization of static field " + wholeFieldName
									+ " found, but no such field declared");
				}

				// clone part of the asm code
				InsnList initASMCode = simpleInsnListClone(origInitCodeIL,
						firstInitInsn, instr);

				// store the code
				slv.setInitASMCode(initASMCode);

				// prepare first init for next field
				firstInitInsn = instr.getNext();
			}

			// if opcode is return then we are done
			if (AsmHelper.isReturn(instr.getOpcode())) {
				break;
			}
		}
	}

	private Snippet parseSnippets(String className, MethodNode method)
			throws SnippetParserException, ReflectionException,
			ScopeParserException, StaticAnalysisException {

		if (method.invisibleAnnotations == null) {
			throw new SnippetParserException("DiSL anottation for method "
					+ method.name + " is missing");
		}

		if (method.invisibleAnnotations.size() > 1) {
			throw new SnippetParserException("Method " + method.name
					+ " can have only one DiSL anottation");
		}

		if ((method.access & Opcodes.ACC_STATIC) == 0) {
			throw new SnippetParserException("Method " + method.name
					+ " should be declared as static");
		}

		if (!Type.getReturnType(method.desc).equals(Type.VOID_TYPE)) {
			throw new SnippetParserException("Method " + method.name
					+ " cannot return value");
		}

		AnnotationNode annotation = method.invisibleAnnotations.get(0);

		MethodAnnotationData annotData = parseMethodAnnotation(annotation);

		// if this is unknown annotation
		if (!annotData.isKnown()) {
			throw new SnippetParserException("Method " + method.name
					+ " has unsupported DiSL annotation");
		}

		// ** marker **

		// get marker class
		Class<?> markerClass = ReflectionHelper.resolveClass(annotData
				.getMarker());

		// try to instantiate marker WITH Parameter as an argument
		Marker marker = (Marker) ReflectionHelper.tryCreateInstance(
				markerClass, new Parameter(annotData.getParam()));

		// instantiate marker WITHOUT Parameter as an argument
		// throws exception if it is not possible to create an instance
		if (marker == null) {
			marker = (Marker) ReflectionHelper.createInstance(markerClass);
		}

		// ** scope **
		Scope scope = new ScopeImpl(annotData.getScope());

		// ** parse used static analysis **
		Analysis analysis = parseAnalysis(method.desc);

		// detect empty stippets
		if (AsmHelper.containsOnlyReturn(method.instructions)) {
			throw new SnippetParserException("Method " + method.name
					+ " cannot be empty");
		}

		// ** create unprocessed code holder class **
		UnprocessedSnippetCode uscd = new UnprocessedSnippetCode(className,
				method.name, method.instructions, method.tryCatchBlocks,
				analysis.getStaticAnalyses(), analysis.usesDynamicAnalysis());

		// whole snippet
		return new Snippet(annotData.getType(), marker, scope,
				annotData.getOrder(), uscd);
	}

	// data holder for parseMethodAnnotation methods
	private class MethodAnnotationData {

		private boolean known;
		private Class<?> type;
		private Type marker;
		private String param;
		private String scope;
		private int order;

		public MethodAnnotationData() {
			this.known = false;
		}

		public MethodAnnotationData(Class<?> type, Type marker, String param,
				String scope, int order) {
			super();

			this.known = true;
			this.type = type;
			this.marker = marker;
			this.param = param;
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

		public String getParam() {
			return param;
		}

		public String getScope() {
			return scope;
		}

		public int getOrder() {
			return order;
		}
	}

	private MethodAnnotationData parseMethodAnnotation(AnnotationNode annotation) {

		Type annotationType = Type.getType(annotation.desc);

		// after annotation
		if (annotationType.equals(Type.getType(After.class))) {
			return parseMethodAnnotFields(After.class, annotation,
					annotation.values);
		}

		// after normal execution annotation
		if (annotationType.equals(Type.getType(AfterReturning.class))) {
			return parseMethodAnnotFields(AfterReturning.class, annotation,
					annotation.values);
		}

		// after abnormal execution annotation
		if (annotationType.equals(Type.getType(AfterThrowing.class))) {
			return parseMethodAnnotFields(AfterThrowing.class, annotation,
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

		Type marker = null;
		String param = ""; // default
		String scope = null;
		Integer order = 100; // default

		while (it.hasNext()) {

			String name = (String) it.next();

			if (name.equals("marker")) {

				marker = (Type) it.next();
				continue;
			}

			if (name.equals("param")) {

				param = (String) it.next();
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

		if (marker == null || param == null || scope == null || order == null) {

			throw new DiSLFatalException("Missing field in annotation "
					+ type.toString()
					+ ". This may happen if annotation class is changed but"
					+ " parser is not.");
		}

		return new MethodAnnotationData(type, marker, param, scope, order);
	}

	private class Analysis {

		private Set<String> staticAnalyses;
		private boolean usesDynamicAnalysis;

		public Analysis(Set<String> staticAnalyses, boolean usesDynamicAnalysis) {
			super();
			this.staticAnalyses = staticAnalyses;
			this.usesDynamicAnalysis = usesDynamicAnalysis;
		}

		public Set<String> getStaticAnalyses() {
			return staticAnalyses;
		}

		public boolean usesDynamicAnalysis() {
			return usesDynamicAnalysis;
		}
	}

	private Analysis parseAnalysis(String methodDesc)
			throws ReflectionException, StaticAnalysisException {

		Set<String> knownStAn = new HashSet<String>();
		boolean usesDynamicAnalysis = false;

		for (Type argType : Type.getArgumentTypes(methodDesc)) {

			// skip dynamic analysis class - don't check anything
			if (argType.equals(Type.getType(DynamicContext.class))) {
				usesDynamicAnalysis = true;
				continue;
			}

			Class<?> argClass = ReflectionHelper.resolveClass(argType);

			// static analysis should implement analysis interface
			if (!implementsStaticAnalysis(argClass)) {
				throw new StaticAnalysisException(argClass.getName()
						+ " does not implement StaticAnalysis interface and"
						+ " cannot be used as disl method parameter");
			}

			knownStAn.add(argType.getInternalName());
		}

		return new Analysis(knownStAn, usesDynamicAnalysis);
	}

	/**
	 * Searches for StaticAnalysis interface. Searches through whole class
	 * hierarchy.
	 * 
	 * @param classToSearch
	 */
	private boolean implementsStaticAnalysis(Class<?> classToSearch) {

		// through whole hierarchy...
		while (classToSearch != null) {

			// ...through all interfaces...
			for (Class<?> iface : classToSearch.getInterfaces()) {

				// ...search for StaticAnalysis interface
				if (iface.equals(StaticAnalysis.class)) {
					return true;
				}
			}

			classToSearch = classToSearch.getSuperclass();
		}

		return false;
	}
}
