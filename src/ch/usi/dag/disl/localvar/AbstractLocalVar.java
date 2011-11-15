package ch.usi.dag.disl.localvar;

import org.objectweb.asm.Type;

public abstract class AbstractLocalVar {

	public final static String NAME_DELIM = ".";
	
	private String className;
	private String fieldName;
	private Type type;
	
	public AbstractLocalVar(String className, String fieldName, Type type) {
		super();
		this.className = className;
		this.fieldName = fieldName;
		this.type = type;
	}

	public String getID() {
		return className + NAME_DELIM + fieldName;
	}
	
	public String getOwner() {
		return className;
	}

	public String getName() {
		return fieldName;
	}
	
	public Type getType() {
		return type;
	}
}
