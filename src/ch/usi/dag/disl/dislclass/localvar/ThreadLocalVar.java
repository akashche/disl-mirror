package ch.usi.dag.disl.dislclass.localvar;


public class ThreadLocalVar {

	public final static String NAME_DELIM = ".";
	
	private String className;
	private String fieldName;
	
	public ThreadLocalVar(String className, String fieldName) {
		
		super();
		this.className = className;
		this.fieldName = fieldName;
	}
	
	public String getID() {
		return className + NAME_DELIM + fieldName;
	}

}
