package ch.usi.dag.disl.dislclass.parser;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import ch.usi.dag.disl.dislclass.annotation.After;
import ch.usi.dag.disl.dislclass.annotation.AfterReturning;
import ch.usi.dag.disl.dislclass.annotation.AfterThrowing;
import ch.usi.dag.disl.dislclass.annotation.Before;
import ch.usi.dag.disl.dislclass.snippet.Snippet;
import ch.usi.dag.disl.dislclass.snippet.SnippetUnprocessedCode;
import ch.usi.dag.disl.dislclass.snippet.marker.Marker;
import ch.usi.dag.disl.dislclass.snippet.marker.Parameter;
import ch.usi.dag.disl.dislclass.snippet.scope.Scope;
import ch.usi.dag.disl.dislclass.snippet.scope.ScopeImpl;
import ch.usi.dag.disl.dynamicinfo.DynamicContext;
import ch.usi.dag.disl.exception.DiSLFatalException;
import ch.usi.dag.disl.exception.MarkerException;
import ch.usi.dag.disl.exception.ParserException;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.ScopeParserException;
import ch.usi.dag.disl.exception.SnippetParserException;
import ch.usi.dag.disl.exception.StaticInfoException;
import ch.usi.dag.disl.guard.SnippetGuard;
import ch.usi.dag.disl.staticinfo.analysis.StaticAnalysis;
import ch.usi.dag.disl.util.AsmHelper;
import ch.usi.dag.disl.util.Constants;
import ch.usi.dag.disl.util.ReflectionHelper;

/**
 * The parser takes annotated java file as input and creates Snippet classes
 */
public class SnippetParser extends AbstractParser {

	private List<Snippet> snippets = new LinkedList<Snippet>();

	public List<Snippet> getSnippets() {

		return snippets;
	}
	
	public void parse(ClassNode classNode) throws ParserException,
			SnippetParserException, ReflectionException, ScopeParserException,
			StaticInfoException, MarkerException {

		// NOTE: this method can be called many times

		// process local variables
		processLocalVars(classNode);

		for (MethodNode method : classNode.methods) {

			// skip the constructor
			if (method.name.equals(Constants.CONSTRUCTOR_NAME)) {
				continue;
			}

			// skip static initializer
			if (method.name.equals(Constants.STATIC_INIT_NAME)) {
				continue;
			}

			snippets.add(parseSnippet(classNode.name, method));
		}
	}
	
	// parse snippet
	private Snippet parseSnippet(String className, MethodNode method)
			throws SnippetParserException, ReflectionException,
			ScopeParserException, StaticInfoException, MarkerException {

		// check annotation
		if (method.invisibleAnnotations == null) {
			throw new SnippetParserException("DiSL anottation for method "
					+ className + "." + method.name + " is missing");
		}

		// check only one annotation
		if (method.invisibleAnnotations.size() > 1) {
			throw new SnippetParserException("Method " + className + "."
					+ method.name + " can have only one DiSL anottation");
		}

		// check static
		if ((method.access & Opcodes.ACC_STATIC) == 0) {
			throw new SnippetParserException("Method " + className + "."
					+ method.name + " should be declared as static");
		}

		// check return type
		if (!Type.getReturnType(method.desc).equals(Type.VOID_TYPE)) {
			throw new SnippetParserException("Method " + className + "."
					+ method.name + " cannot return value");
		}

		AnnotationNode annotation = method.invisibleAnnotations.get(0);

		SnippetAnnotationData annotData = 
			parseMethodAnnotation(className + "." + method.name, annotation);

		// ** marker **
		Marker marker = 
			getMarker(annotData.marker, annotData.param);

		// ** scope **
		Scope scope = new ScopeImpl(annotData.scope);

		// ** guard **
		SnippetGuard guard = 
			(SnippetGuard) ParserHelper.getGuard(annotData.guard);
		
		// ** parse used static analysis **
		Analysis analysis = parseAnalysis(method.desc);

		// ** checks **

		// detect empty snippets
		if (AsmHelper.containsOnlyReturn(method.instructions)) {
			throw new SnippetParserException("Method " + className + "."
					+ method.name + " cannot be empty");
		}

		// analysis arguments (local variables 1, 2, ...) cannot be stored or
		// overwritten, may be used only in method calls
		usesAnalysisProperly(className, method.name, method.desc,
				method.instructions);

		// values of dynamic analysis method arguments should be directly passed
		// constants
		passesConstsToDynamicAnalysis(className, method.name,
				method.instructions);

		// ** create unprocessed code holder class **
		// code is processed after everything is parsed
		SnippetUnprocessedCode uscd = new SnippetUnprocessedCode(className,
				method.name, method.instructions, method.tryCatchBlocks,
				analysis.getStaticAnalyses(), analysis.usesDynamicAnalysis(),
				annotData.dynamicBypass);

		// return whole snippet
		return new Snippet(annotData.type, marker, scope, guard,
				annotData.order, uscd);
	}

	private SnippetAnnotationData parseMethodAnnotation(String fullMethodName,
			AnnotationNode annotation) throws SnippetParserException {

		Type annotationType = Type.getType(annotation.desc);

		// after annotation
		if (annotationType.equals(Type.getType(After.class))) {
			return parseMethodAnnotFields(After.class, annotation);
		}

		// after normal execution annotation
		if (annotationType.equals(Type.getType(AfterReturning.class))) {
			return parseMethodAnnotFields(AfterReturning.class, annotation);
		}

		// after abnormal execution annotation
		if (annotationType.equals(Type.getType(AfterThrowing.class))) {
			return parseMethodAnnotFields(AfterThrowing.class, annotation);
		}

		// before annotation
		if (annotationType.equals(Type.getType(Before.class))) {
			return parseMethodAnnotFields(Before.class, annotation);
		}

		// unknown annotation
		throw new SnippetParserException("Method " + fullMethodName
				+ " has unsupported DiSL annotation");
	}

	// data holder for AnnotationParser
	private static class SnippetAnnotationData {

		public Class<?> type;
		
		// annotation values
		public Type marker = null;
		public String param = null; // default
		public String scope = null;
		public Type guard = null; // default
		public int order = 100; // default
		public boolean dynamicBypass = false; // default
		
		public SnippetAnnotationData(Class<?> type) {
			this.type = type;
		}
	}
	
	private SnippetAnnotationData parseMethodAnnotFields(Class<?> type,
			AnnotationNode annotation) {

		SnippetAnnotationData sad = new SnippetAnnotationData(type);
		ParserHelper.parseAnnotation(sad, annotation);
		
		if (sad.marker == null || sad.scope == null) {

			throw new DiSLFatalException("Missing attribute in annotation "
					+ type.toString()
					+ ". This may happen if annotation class is changed but"
					+ " data holder class is not.");
		}

		return sad;
	}
	
	private Marker getMarker(Type markerType, String markerParam)
			throws ReflectionException, MarkerException {
		
		// get marker class
		Class<?> markerClass = ReflectionHelper.resolveClass(markerType);

		// instantiate marker WITHOUT Parameter as an argument
		if(markerParam == null) {
			try {
				return (Marker) ReflectionHelper.createInstance(markerClass);
			}
			catch(ReflectionException e) {
				
				if(e.getCause() instanceof NoSuchMethodException) {
					throw new MarkerException("Marker " + markerClass.getName()
							+ " requires \"param\" annotation attribute"
							+ " declared",
							e);
				}
				
				throw e;
			}
		}

		// try to instantiate marker WITH Parameter as an argument
		try {
			return (Marker) ReflectionHelper.createInstance(markerClass,
					new Parameter(markerParam));
		}
		catch(ReflectionException e) {
			
			if(e.getCause() instanceof NoSuchMethodException) {
				throw new MarkerException("Marker " + markerClass.getName()
						+ " does not support \"param\" attribute", e);
			}
			
			throw e;
		}
	}

	private static class Analysis {

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
			throws ReflectionException, StaticInfoException {

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
				throw new StaticInfoException(argClass.getName()
						+ " does not implement StaticAnalysis interface and"
						+ " cannot be used as advice method parameter");
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

	private void usesAnalysisProperly(String className, String methodName,
			String methodDescriptor, InsnList instructions)
			throws SnippetParserException {

		Type[] types = Type.getArgumentTypes(methodDescriptor);
		int maxArgIndex = 0;

		// count the max index of arguments
		for (int i = 0; i < types.length; i++) {

			// add number of occupied slots
			maxArgIndex += types[i].getSize();
		}

		// The following code assumes that all disl advices are static
		for (AbstractInsnNode instr : instructions.toArray()) {

			switch (instr.getOpcode()) {
			// test if the analysis is stored somewhere else
			case Opcodes.ALOAD: {

				int local = ((VarInsnNode) instr).var;

				if (local < maxArgIndex
						&& instr.getNext().getOpcode() == Opcodes.ASTORE) {
					throw new SnippetParserException("In advice " + className
							+ "." + methodName + " - advice parameter"
							+ " (analysis) cannot be stored into local"
							+ " variable");
				}

				break;
			}
				// test if something is stored in the analysis
			case Opcodes.ASTORE: {

				int local = ((VarInsnNode) instr).var;

				if (local < maxArgIndex) {
					throw new SnippetParserException("In advice " + className
							+ "." + methodName + " - advice parameter"
							+ " (analysis) cannot overwritten");
				}

				break;
			}
			}
		}
	}

	/**
	 * Checks if dynamic analysis methods contains only
	 */
	private void passesConstsToDynamicAnalysis(String className,
			String methodName, InsnList instructions)
			throws SnippetParserException {

		for (AbstractInsnNode instr : instructions.toArray()) {

			// it is invocation...
			if (instr.getOpcode() != Opcodes.INVOKEVIRTUAL) {
				continue;
			}

			MethodInsnNode invoke = (MethodInsnNode) instr;

			// ... of dynamic analysis
			if (!invoke.owner
					.equals(Type.getInternalName(DynamicContext.class))) {
				continue;
			}

			AbstractInsnNode secondOperand = instr.getPrevious();
			AbstractInsnNode firstOperand = secondOperand.getPrevious();

			// first operand test
			switch (firstOperand.getOpcode()) {
			case Opcodes.ICONST_M1:
			case Opcodes.ICONST_0:
			case Opcodes.ICONST_1:
			case Opcodes.ICONST_2:
			case Opcodes.ICONST_3:
			case Opcodes.ICONST_4:
			case Opcodes.ICONST_5:
			case Opcodes.BIPUSH:
				break;

			default:
				throw new SnippetParserException("In advice " + className + "."
						+ methodName + " - pass the first (pos)"
						+ " argument of a dynamic context method direcltly."
						+ " ex: getStackValue(1, int.class)");
			}

			// second operand test
			if (AsmHelper.getClassType(secondOperand) == null) {
				throw new SnippetParserException("In advice " + className + "."
						+ methodName + " - pass the second (type)"
						+ " argument of a dynamic context method direcltly."
						+ " ex: getStackValue(1, int.class)");
			}
		}
	}
}
