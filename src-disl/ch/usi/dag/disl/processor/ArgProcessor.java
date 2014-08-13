package ch.usi.dag.disl.processor;

import java.util.List;

import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticContextGenException;
import ch.usi.dag.disl.localvar.LocalVars;

public class ArgProcessor {

	private String name;
	private List<ArgProcessorMethod> methods;

	public ArgProcessor(String name, List<ArgProcessorMethod> methods) {
		super();
		this.name = name;
		this.methods = methods;
	}

	public String getName() {
		return name;
	}
	
	public List<ArgProcessorMethod> getMethods() {
		return methods;
	}
	
	public void init(LocalVars allLVs) throws StaticContextGenException,
			ReflectionException {
		
		for(ArgProcessorMethod method : methods) {
			method.init(allLVs);
		}
	}
}
