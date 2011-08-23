package ch.usi.dag.disl.dislclass.parser;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.dislclass.processor.Proc;
import ch.usi.dag.disl.exception.DiSLException;

public class ProcessorParser extends AbstractParser {

	// first map argument is ASM type representing processor class where the
	// processor is defined
	private Map<Type, Proc> processors = new HashMap<Type, Proc>();

	public Map<Type, Proc> getProcessors() {

		return processors;
	}
	
	protected void parseMethodContent(String className, MethodNode method)
			throws DiSLException {
		// TODO ! processors
		
	}
}
