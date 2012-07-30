package ch.usi.dag.dislreserver.reflectiveinfo;

public interface FieldInfo {

	public String getName();

	public int getModifiers();

	public String getType();

	public boolean isPublic();
}
