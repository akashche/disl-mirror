package ch.usi.dag.disl.dislclass.parser;

import java.util.EnumSet;
import java.util.HashMap;
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
	
	// data holder for AnnotationParser
	private static class ProcessorAnnotationData {

		public Type guard = null; // default
	}
	
	public void parse(ClassNode classNode) throws ParserException,
			ProcessorParserException, ReflectionException {

		// NOTE: this method can be called many times

		// there is only one annotation - processor annotation
		AnnotationNode annotation = 
			(AnnotationNode) classNode.invisibleAnnotations.get(0);
		
		// parse processor annotation
		ProcessorAnnotationData pad = ParserHelper.parseAnnotation(annotation,
				new ProcessorAnnotationData());

		// ** guard **
		ProcessorGuard guard = 
			(ProcessorGuard) ParserHelper.getGuard(pad.guard);
		
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
		
		// ** parse processor method arguments **
		ProcArgType methodArgType = parseProcMethodArgs(
				className + "." + method.name, method.desc);

		EnumSet<ProcArgType> allTypes = EnumSet.of(methodArgType);
		
		// ** parse processor method annotation **
		ProcMethodAnnotationsData pmad = 
			parseMethodAnnotations(fullMethodName, method.invisibleAnnotations);

		// TODO ! add process also
		
		// ** guard **
		ProcessorMethodGuard guard = 
			(ProcessorMethodGuard) ParserHelper.getGuard(pmad.guard);
		
		// ** create unprocessed code holder class **
		// code is processed after everything is parsed
		UnprocessedCode ucd = new UnprocessedCode(method.instructions,
				method.tryCatchBlocks);

		// return whole processor method
		return new ProcMethod(allTypes, guard, ucd);

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
	private static class ProcMethodAnnotationsData {

		public Type guard = null;
	}

	private ProcMethodAnnotationsData parseMethodAnnotations(String fullMethodName,
			List<AnnotationNode> invisibleAnnotations)
			throws ProcessorParserException {

		// TODO ! add process also
		
		// check annotation
		if (invisibleAnnotations == null) {
			return new ProcMethodAnnotationsData();
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

	private ProcMethodAnnotationsData parseMethodAnnotFields(
			AnnotationNode annotation) {

		ProcMethodAnnotationsData pmad = ParserHelper.parseAnnotation(
				annotation, new ProcMethodAnnotationsData());
		
		if(pmad.guard == null) {

			throw new DiSLFatalException("Missing attribute in annotation "
					+ Type.getType(annotation.desc).toString()
					+ ". This may happen if annotation class is changed but"
					+ " data holder class is not.");
		}
		
		return pmad;
	}
}
