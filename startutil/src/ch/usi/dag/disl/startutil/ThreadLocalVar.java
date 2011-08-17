package ch.usi.dag.disl.startutil;

import org.objectweb.asm.Type;

public class ThreadLocalVar {

	private String name;
	private Type type;
	private String defaultValue;
	private boolean inheritable;
	
	public ThreadLocalVar(String name, Type type, boolean inheritable) {
		super();
		this.name = name;
		this.type = type;
		this.inheritable = inheritable;
	}
	
	public String getName() {
		return name;
	}
	
	public Type getType() {
		return type;
	}
	
	public String getTypeAsDesc() {
		return type.getDescriptor();
	}
	
	public String getDefaultValue() {
		return defaultValue;
	}
	
	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public boolean isInheritable() {
		return inheritable;
	}
	
	public String isInheritableToStr() {
		return Boolean.toString(inheritable);
	}
}
