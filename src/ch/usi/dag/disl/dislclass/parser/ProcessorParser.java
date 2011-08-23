package ch.usi.dag.disl.dislclass.parser;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.dislclass.code.UnprocessedCode;
import ch.usi.dag.disl.dislclass.processor.Proc;
import ch.usi.dag.disl.dislclass.processor.ProcArgType;
import ch.usi.dag.disl.dislclass.processor.ProcMethod;
import ch.usi.dag.disl.exception.ParserException;
import ch.usi.dag.disl.exception.ProcessorParserException;
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
			ProcessorParserException {

		// NOTE: this method can be called many times

		// process local variables
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
		
		// TODO ! processors - test
		processors.put(Type.getType(classNode.name),
				new Proc(classNode.name, methods));
	}
	
	private ProcMethod parseProcessorMethod(String className, MethodNode method)
			throws ProcessorParserException {

		// check static
		if ((method.access & Opcodes.ACC_STATIC) == 0) {
			throw new ProcessorParserException("Method " + className + "."
					+ method.name + " should be declared as static");
		}

		// check return type
		if (!Type.getReturnType(method.desc).equals(Type.VOID_TYPE)) {
			throw new ProcessorParserException("Method " + className + "."
					+ method.name + " cannot return value");
		}
		
		// detect empty processors
		if (AsmHelper.containsOnlyReturn(method.instructions)) {
			throw new ProcessorParserException("Method " + className + "."
					+ method.name + " cannot be empty");
		}
		
		// ** parse processor method arguments **
		ProcArgType methodArgType = parseProcMethodArgs(method.desc);
		
		// ** create unprocessed code holder class **
		// code is processed after everything is parsed
		UnprocessedCode ucd = new UnprocessedCode(method.instructions,
				method.tryCatchBlocks);

		// return whole processor method
		return new ProcMethod(methodArgType, ucd);

	}

	private ProcArgType parseProcMethodArgs(String desc) {

		// TODO ! processors - implement
		return null;
	}
}
