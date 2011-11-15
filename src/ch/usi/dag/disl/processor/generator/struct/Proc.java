package ch.usi.dag.disl.processor.generator.struct;

import java.util.List;

import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticContextGenException;
import ch.usi.dag.disl.guard.ProcessorGuard;
import ch.usi.dag.disl.localvar.LocalVars;

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
	
	public void init(LocalVars allLVs) throws StaticContextGenException,
			ReflectionException {
		
		for(ProcMethod method : methods) {
			method.init(allLVs);
		}
	}
}
