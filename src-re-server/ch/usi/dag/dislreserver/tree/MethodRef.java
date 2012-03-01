package ch.usi.dag.dislreserver.tree;

public class MethodRef {

	private String name;
	private String desc;

	public MethodRef(String name, String desc) {
		this.name = name;
		this.desc = desc;
	}

	public String getName() {
		return name;
	}

	public String getDesc() {
		return desc;
	}

}
