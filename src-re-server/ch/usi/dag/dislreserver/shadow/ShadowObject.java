package ch.usi.dag.dislreserver.shadow;

public class ShadowObject {

	final private long objectId;
	final private ShadowClass sClass;

	private Object shadowState;

	ShadowObject(long net_ref) {

		this(net_ref, ShadowClassTable.get(NetReferenceHelper
				.get_class_id(net_ref)));
	}

	ShadowObject(long net_ref, ShadowClass sClass) {

		this.objectId = NetReferenceHelper.get_object_id(net_ref);
		this.sClass = sClass;
		this.shadowState = null;
	}

	public long getId() {
		return objectId;
	}

	public ShadowClass getSClass() {
		return sClass;
	}

	public Object get() {
		return shadowState;
	}

	public void set(Object shadowState) {
		this.shadowState = shadowState;
	}

}
