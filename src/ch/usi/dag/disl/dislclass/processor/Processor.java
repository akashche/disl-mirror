package ch.usi.dag.disl.dislclass.processor;

import java.util.List;

import ch.usi.dag.disl.dislclass.localvar.LocalVars;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticAnalysisException;

public class Processor {

	private List<ProcessorMethod> methods;

	public Processor(List<ProcessorMethod> methods) {
		super();
		this.methods = methods;
	}

	public List<ProcessorMethod> getMethods() {
		return methods;
	}
	
	public void init(LocalVars allLVs) throws StaticAnalysisException,
			ReflectionException {
		
		for(ProcessorMethod method : methods) {
			method.init(allLVs);
		}
	}
}
