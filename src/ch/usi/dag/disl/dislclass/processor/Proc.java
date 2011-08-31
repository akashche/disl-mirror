package ch.usi.dag.disl.dislclass.processor;

import java.util.List;

import ch.usi.dag.disl.dislclass.localvar.LocalVars;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticInfoException;
import ch.usi.dag.disl.guard.ProcessorGuard;

public class Proc {

	private String name;
	private ProcessorGuard guard;
	private List<ProcMethod> methods;

	public Proc(String name, ProcessorGuard guard, List<ProcMethod> methods) {
		super();
		this.name = name;
		this.guard = guard;
		this.methods = methods;
	}

	public String getName() {
		return name;
	}
	
	public ProcessorGuard getGuard() {
		return guard;
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
