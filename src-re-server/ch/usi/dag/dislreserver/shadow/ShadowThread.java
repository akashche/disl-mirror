package ch.usi.dag.dislreserver.shadow;

public class ShadowThread extends ShadowObject {

	private String name;
	private boolean isDaemon;

	public ShadowThread(long net_ref, String name, boolean isDaemon) {
		super(net_ref);

		this.name = name;
		this.isDaemon = isDaemon;
	}

	public String getName() {
		return name;
	}

	public boolean isDaemon() {
		return isDaemon;
	}

}
