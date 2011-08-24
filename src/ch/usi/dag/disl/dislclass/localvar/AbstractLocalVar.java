package ch.usi.dag.disl.dislclass.localvar;

public abstract class AbstractLocalVar {

	public final static String NAME_DELIM = ".";
	
	private String className;
	private String fieldName;
	
	public AbstractLocalVar(String className, String fieldName) {
		super();
		this.className = className;
		this.fieldName = fieldName;
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
}
