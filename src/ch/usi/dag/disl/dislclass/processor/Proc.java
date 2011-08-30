package ch.usi.dag.disl.dislclass.processor;

import java.util.List;

import ch.usi.dag.disl.dislclass.localvar.LocalVars;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticInfoException;

public class Proc {

	private String name;
	private List<ProcMethod> methods;

	public Proc(String name, List<ProcMethod> methods) {
		super();
		this.name = name;
		this.methods = methods;
	}

	public String getName() {
		return name;
	}
	
	public List<ProcMethod> getMethods() {
		return methods;
	}
	
	public void init(LocalVars allLVs) throws StaticInfoException,
			ReflectionException {
		
		for(ProcMethod method : methods) {
			method.init(allLVs);
		}
	}
}
