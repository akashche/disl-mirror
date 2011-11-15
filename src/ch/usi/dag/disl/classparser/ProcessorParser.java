package ch.usi.dag.disl.classparser;

import java.util.Collection;
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

import ch.usi.dag.disl.annotation.Guarded;
import ch.usi.dag.disl.annotation.ProcessAlso;
import ch.usi.dag.disl.coderep.UnprocessedCode;
import ch.usi.dag.disl.exception.DiSLFatalException;
import ch.usi.dag.disl.exception.ParserException;
import ch.usi.dag.disl.exception.ProcessorParserException;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.guard.ProcessorMethodGuard;
import ch.usi.dag.disl.processor.generator.struct.Proc;
import ch.usi.dag.disl.processor.generator.struct.ProcArgType;
import ch.usi.dag.disl.processor.generator.struct.ProcMethod;
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
			throw new ProcessorParserException("ArgsProcessor class "
					+ classNode.name + " should contain methods");
		}
		
		Type processorClassType = Type.getType("L" + classNode.name + ";"); 
		
		processors.put(processorClassType, 
				new Proc(classNode.name, methods));
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
			throw new ProcessorParserException("Method " + fullMethodName
					 + " cannot be empty");
		}
		
		// no exception can be thrown
		if(! method.exceptions.isEmpty()) {
			throw new ProcessorParserException("Method " + fullMethodName
					 + " cannot throw any exception");
		}
		
		// ** parse processor method arguments **
		PMArgData pmArgData = parseProcMethodArgs(
				className + "." + method.name, method.desc);

		// all processed types - add method type
		EnumSet<ProcArgType> allProcessedTypes = 
			EnumSet.of(pmArgData.getType());
		
		// ** parse processor method annotation **
		ProcMethodAnnotationsData pmad = 
			parseMethodAnnotations(fullMethodName, method.invisibleAnnotations);

		// check if process also annotation contains only valid types
		checkProcessAlsoSetValidity(fullMethodName, pmArgData.getType(),
				pmad.processAlsoTypes);
		
		// add types from process also annotation
		allProcessedTypes.addAll(pmad.processAlsoTypes);
		
		// ** guard **
		ProcessorMethodGuard guard = 
			(ProcessorMethodGuard) ParserHelper.getGuard(pmad.guard);
		
		// ** create unprocessed code holder class **
		// code is processed after everything is parsed
		UnprocessedCode ucd = new UnprocessedCode(method.instructions,
				method.tryCatchBlocks);

		// return whole processor method
		return new ProcMethod(className, method.name, allProcessedTypes,
				pmArgData.insertTypeName(), guard, ucd);

	}

	private static class PMArgData {
		
		private ProcArgType type;
		private boolean insertTypeName;
		
		public PMArgData(ProcArgType type, boolean insertTypeName) {
			super();
			this.type = type;
			this.insertTypeName = insertTypeName;
		}

		public ProcArgType getType() {
			return type;
		}

		public boolean insertTypeName() {
			return insertTypeName;
		}
	}
	
	private PMArgData parseProcMethodArgs(String methodID, String methodDesc)
			throws ProcessorParserException {

		final int PM_ARGS_STD_COUNT = 3;
		
		final int PM_ARGS_OBJ_MAX_COUNT = 4;
		
		Type[] argTypes = Type.getArgumentTypes(methodDesc);
		
		// not == because of PM_ARGS_OBJ_MAX_COUNT
		if(argTypes.length < PM_ARGS_STD_COUNT) {
			throw new ProcessorParserException(
					"ArgsProcessor method " + methodID +  " should have at least "
					+ PM_ARGS_STD_COUNT + " arguments.");
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
		
		ProcArgType argType = ProcArgType.valueOf(argTypes[2]);
		
		// if the ProcArgType is converted to OBJECT, test that third argument
		// is really Object.class - nothing else is allowed
		if(argType == ProcArgType.OBJECT
				&& ! Type.getType(Object.class).equals(argTypes[2])) {
			
			throw new ProcessorParserException("In method " + methodID + ": " +
					"Only basic types and Object are allowed as the" +
					" third parameter");
		}
		
		// ** non object arg types **
		
		if(argType != ProcArgType.OBJECT) {

			// argument count test
			if(argTypes.length != PM_ARGS_STD_COUNT) {
				throw new ProcessorParserException("ArgsProcessor method "
						+ methodID + " should have "
						+ PM_ARGS_STD_COUNT + " arguments.");
			}
			
			return new PMArgData(argType, false);
		}

		// ** object arg type **
		
		// no additional argument
		if(argTypes.length == PM_ARGS_STD_COUNT) {
			return new PMArgData(argType, false);
		}
		
		// object type argument
		if(argTypes.length == PM_ARGS_OBJ_MAX_COUNT) {
			
			// last argument in object processor (type) has to be String
			if(! Type.getType(String.class).equals(argTypes[3])) {
				throw new ProcessorParserException("In method " + methodID
						+ ": Last argument in object processor (type) has to"
						+ " be String");
			}
			
			return new PMArgData(argType, true);
		}
		
		throw new ProcessorParserException("ArgsProcessor method "
				+ methodID + " should have at most "
				+ PM_ARGS_OBJ_MAX_COUNT + " arguments.");
	}
	
	// data holder for parseMethodAnnotation methods
	private static class ProcMethodAnnotationsData {

		public EnumSet<ProcArgType> processAlsoTypes = 
			EnumSet.noneOf(ProcArgType.class);
		
		public Type guard = null;
	}

	private ProcMethodAnnotationsData parseMethodAnnotations(
			String fullMethodName,
			List<AnnotationNode> invisibleAnnotations)
			throws ProcessorParserException {

		ProcMethodAnnotationsData pmad = new ProcMethodAnnotationsData();
		
		// check no annotation
		if (invisibleAnnotations == null) {
			return pmad;
		}
		
		for(AnnotationNode annotation : invisibleAnnotations) {

			Type annotationType = Type.getType(annotation.desc);

			// guarded annotation
			if (annotationType.equals(Type.getType(Guarded.class))) {
				parseGuardedAnnotation(pmad, annotation);
				continue;
			}
			
			// process also annotation
			if (annotationType.equals(Type.getType(ProcessAlso.class))) {
				parseProcessAlsoAnnotation(pmad, annotation);
				continue;
			}
			
			// unknown annotation
			throw new ProcessorParserException("Method " + fullMethodName
					+ " has unsupported DiSL annotation");
		}
		
		return pmad;
	}

	// NOTE: pmad is modified by this function
	private void parseGuardedAnnotation(
			ProcMethodAnnotationsData pmad, AnnotationNode annotation) {

		ParserHelper.parseAnnotation(pmad, annotation);
		
		if(pmad.guard == null) {

			throw new DiSLFatalException("Missing attribute in annotation "
					+ Type.getType(annotation.desc).toString()
					+ ". This may happen if annotation class is changed but"
					+ " data holder class is not.");
		}
	}
	
	private static class ProcessAlsoAnnotationData {
		
		public Collection<String[]> types = null;
	}
	
	// NOTE: pmad is modified by this function
	private void parseProcessAlsoAnnotation(ProcMethodAnnotationsData pmad,
			AnnotationNode annotation) {

		ProcessAlsoAnnotationData paData = new ProcessAlsoAnnotationData();
		
		ParserHelper.parseAnnotation(paData, annotation);
		
		if(paData.types == null) {

			throw new DiSLFatalException("Missing attribute in annotation "
					+ Type.getType(annotation.desc).toString()
					+ ". This may happen if annotation class is changed but"
					+ " data holder class is not.");
		}
		
		// array is converted to collection
		for(String[] enumType : paData.types) {
		
			// enum is converted to array
			//  - first value is class name
			//  - second value is value name
			ProcessAlso.Type paType = ProcessAlso.Type.valueOf(enumType[1]);
			
			pmad.processAlsoTypes.add(ProcArgType.valueOf(paType));
		}
	}
	
	private void checkProcessAlsoSetValidity(String fullMethodName,
			ProcArgType methodArgType,
			EnumSet<ProcArgType> processAlsoTypes)
			throws ProcessorParserException {
		
		EnumSet<ProcArgType> validSet;

		// valid sets for types
		switch(methodArgType) {
		
		case INT: {
			validSet = EnumSet.of(
					ProcArgType.BOOLEAN,
					ProcArgType.BYTE,
					ProcArgType.SHORT);
			break;
		}
		
		case SHORT: {
			validSet = EnumSet.of(
					ProcArgType.BOOLEAN,
					ProcArgType.BYTE);
			break;
		}
		
		case BYTE: {
			validSet = EnumSet.of(
					ProcArgType.BOOLEAN);
			break;
		}
		
		default: {
			// for other types empty set
			validSet = EnumSet.noneOf(ProcArgType.class);
		}
		}
		
		// create set of non valid types
		EnumSet<ProcArgType> nonValidTypes = processAlsoTypes.clone();
		nonValidTypes.removeAll(validSet);
		
		if(! nonValidTypes.isEmpty()) {
			
			StringBuilder strNonValidTypes = new StringBuilder();
			
			final String STR_DELIM = ", ";
			
			for(ProcArgType paType : nonValidTypes) {
				strNonValidTypes.append(paType.toString() + STR_DELIM);
			}
			
			// remove last STR_DELIM
			int delimSize = STR_DELIM.length();
			strNonValidTypes.delete(strNonValidTypes.length() - delimSize,
					strNonValidTypes.length());
			
			throw new ProcessorParserException(methodArgType.toString() 
					+ " processor in method "
					+ fullMethodName
					+ " cannot process "
					+ strNonValidTypes.toString());
		}
	}
}
