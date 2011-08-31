package ch.usi.dag.disl.dislclass.parser;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.dislclass.annotation.Guarded;
import ch.usi.dag.disl.dislclass.code.UnprocessedCode;
import ch.usi.dag.disl.dislclass.processor.Proc;
import ch.usi.dag.disl.dislclass.processor.ProcArgType;
import ch.usi.dag.disl.dislclass.processor.ProcMethod;
import ch.usi.dag.disl.exception.DiSLFatalException;
import ch.usi.dag.disl.exception.ParserException;
import ch.usi.dag.disl.exception.ProcessorParserException;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.guard.ProcessorGuard;
import ch.usi.dag.disl.guard.ProcessorMethodGuard;
import ch.usi.dag.disl.util.AsmHelper;
import ch.usi.dag.disl.util.Constants;

public class ProcessorParser extends AbstractParser {

	// first map argument is ASM type representing processor class where the
	// processor is defined
	private Map<Type, Proc> processors = new HashMap<Type, Proc>();

	public Map<Type, Proc> getProcessors() {

		return processors;
	}
	
	public void parse(ClassNode classNode) throws ParserException,
			ProcessorParserException, ReflectionException {

		// NOTE: this method can be called many times

		// parse processor annotation
		ProcessorAnnotationData pad = parseProcessorAnnot(classNode);

		// ** guard **
		ProcessorGuard guard = (ProcessorGuard) getGuard(pad.getGuard());
		
		// ** local variables **
		processLocalVars(classNode);

		List<ProcMethod> methods = new LinkedList<ProcMethod>();
		
		for (MethodNode method : classNode.methods) {

			// skip the constructor
			if (method.name.equals(Constants.CONSTRUCTOR_NAME)) {
				continue;
			}

			// skip static initializer
			if (method.name.equals(Constants.STATIC_INIT_NAME)) {
				continue;
			}

			methods.add(parseProcessorMethod(classNode.name, method));
		}
		
		if(methods.isEmpty()) {
			throw new ProcessorParserException("Processor class "
					+ classNode.name + " should contain methods");
		}
		
		Type processorClassType = Type.getType("L" + classNode.name + ";"); 
		
		processors.put(processorClassType, 
				new Proc(classNode.name, guard, methods));
	}
	
	// data holder for parseMethodAnnotation methods
	private class ProcessorAnnotationData {

		public ProcessorAnnotationData(Type guard) {
			super();
			this.guard = guard;
		}

		private Type guard;

		public Type getGuard() {
			return guard;
		}
	}
	
	private ProcessorAnnotationData parseProcessorAnnot(ClassNode classNode) {

		// there is only one annotation - processor annotation
		AnnotationNode annotation = 
			(AnnotationNode) classNode.invisibleAnnotations.get(0);
		
		Iterator<?> it = annotation.values.iterator();

		Type guard = null; // default

		while (it.hasNext()) {

			String name = (String) it.next();

			if (name.equals("guard")) {

				guard = (Type) it.next();
				continue;
			}

			throw new DiSLFatalException("Unknow field " + name
					+ " in annotation "
					+ Type.getType(annotation.desc).toString()
					+ ". This may happen if annotation class is changed but"
					+ " parser is not.");
		}

		return new ProcessorAnnotationData(guard);
	}

	private ProcMethod parseProcessorMethod(String className, MethodNode method)
			throws ProcessorParserException, ReflectionException {

		String fullMethodName = className + "." + method.name;
		
		// check static
		if ((method.access & Opcodes.ACC_STATIC) == 0) {
			throw new ProcessorParserException("Method " + fullMethodName
					+ " should be declared as static");
		}

		// check return type
		if (!Type.getReturnType(method.desc).equals(Type.VOID_TYPE)) {
			throw new ProcessorParserException("Method " + fullMethodName
					+ " cannot return value");
		}
		
		// detect empty processors
		if (AsmHelper.containsOnlyReturn(method.instructions)) {
			throw new ProcessorParserException("Method "+ fullMethodName
					 + " cannot be empty");
		}
		
		// ** parse processor method annotation **
		MethodAnnotationsData mad = 
			parseMethodAnnotations(fullMethodName, method.invisibleAnnotations);

		// ** guard **
		ProcessorMethodGuard guard = 
			(ProcessorMethodGuard) getGuard(mad.getGuard());
		
		// ** parse processor method arguments **
		ProcArgType methodArgType = parseProcMethodArgs(
				className + "." + method.name, method.desc);
		
		// ** create unprocessed code holder class **
		// code is processed after everything is parsed
		UnprocessedCode ucd = new UnprocessedCode(method.instructions,
				method.tryCatchBlocks);

		// return whole processor method
		return new ProcMethod(methodArgType, guard, ucd);

	}

	private ProcArgType parseProcMethodArgs(String methodID, String methodDesc)
			throws ProcessorParserException {

		final int PM_ARGS_COUNT = 3;
		
		Type[] argTypes = Type.getArgumentTypes(methodDesc);
		
		if(argTypes.length != PM_ARGS_COUNT) {
			throw new ProcessorParserException(
					"Processor method should have " + PM_ARGS_COUNT
					+ " arguments.");
		}
		
		// first position argument has to be integer
		if(! Type.INT_TYPE.equals(argTypes[0])) {
			throw new ProcessorParserException("In method " + methodID + ": " +
					"First (position) processor method argument has to be int");
		}
		
		// second count argument has to be integer
		if(! Type.INT_TYPE.equals(argTypes[1])) {
			throw new ProcessorParserException("In method " + methodID + ": " +
					"Second (count) processor method argument has to be int");
		}
		
		ProcArgType result = ProcArgType.valueOf(argTypes[2]);
		
		// if the ProcArgType is converted to OBJECT, test that third argument
		// is really Object.class - nothing else is allowed
		if(result == ProcArgType.OBJECT
				&& ! Type.getType(Object.class).equals(argTypes[2])) {
			
			throw new ProcessorParserException("In method " + methodID + ": " +
					"Only basic types, Object and String are allowed as the" +
					" third parameter");
		}
		
		return result;
	}
	
	// data holder for parseMethodAnnotation methods
	private class MethodAnnotationsData {

		private Type guard;

		public MethodAnnotationsData(Type guard) {
			super();
			this.guard = guard;
		}
		
		public Type getGuard() {
			return guard;
		}
	}

	private MethodAnnotationsData parseMethodAnnotations(String fullMethodName,
			List<AnnotationNode> invisibleAnnotations)
			throws ProcessorParserException {

		// TODO ! add process also
		
		// check annotation
		if (invisibleAnnotations == null) {
			throw new ProcessorParserException("DiSL anottation for method "
					+ fullMethodName + " is missing");
		}

		// check only one annotation
		if (invisibleAnnotations.size() > 1) {
			throw new ProcessorParserException("Method " + fullMethodName
					+ " can have only one DiSL anottation");
		}
		
		AnnotationNode annotation = invisibleAnnotations.get(0);
		
		Type annotationType = Type.getType(annotation.desc);

		// after annotation
		if (annotationType.equals(Type.getType(Guarded.class))) {
			return parseMethodAnnotFields(annotation);
		}

		// unknown annotation
		throw new ProcessorParserException("Method " + fullMethodName
				+ " has unsupported DiSL annotation");
	}

	// TODO ! create general parser
	private MethodAnnotationsData parseMethodAnnotFields(
			AnnotationNode annotation) {

		Iterator<?> it = annotation.values.iterator();

		Type guard = null; // default

		while (it.hasNext()) {

			String name = (String) it.next();

			if (name.equals("guard")) {

				guard = (Type) it.next();
				continue;
			}

			throw new DiSLFatalException("Unknow field " + name
					+ " in annotation "
					+ Type.getType(annotation.desc).toString()
					+ ". This may happen if annotation class is changed but"
					+ " parser is not.");
		}

		return new MethodAnnotationsData(guard);
	}
}
