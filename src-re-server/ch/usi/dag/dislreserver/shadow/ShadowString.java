package ch.usi.dag.dislreserver.shadow;

public class ShadowString extends ShadowObject {

	private String value;

	public ShadowString(long net_ref, String value) {
		super(net_ref);
		this.value = value;
	}

	@Override
	public String toString() {
		return value;
	}

}
