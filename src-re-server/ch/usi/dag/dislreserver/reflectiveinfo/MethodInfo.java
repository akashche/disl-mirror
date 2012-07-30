package ch.usi.dag.dislreserver.reflectiveinfo;

public interface MethodInfo {

	public String getName();

	public int getModifiers();

	public String getReturnType();

	public String[] getParameterTypes();

	public String[] getExceptionTypes();

	public boolean isPublic();
}
