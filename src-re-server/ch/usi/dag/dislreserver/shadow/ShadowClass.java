package ch.usi.dag.dislreserver.shadow;

public abstract class ShadowClass extends ShadowObject {

	private int classId;
	private ShadowObject classLoader;

	protected ShadowClass(long net_ref, ShadowObject classLoader) {
		super(net_ref, null);

		this.classId = NetReferenceHelper.get_class_id(net_ref);
		this.classLoader = classLoader;
	}

	public final int getClassId() {
		return classId;
	}

	public final ShadowObject getShadowClassLoader() {
		return classLoader;
	}

	public abstract boolean isArray();

	public abstract ShadowClass getComponentType();

	public abstract boolean isInstance(ShadowObject obj);

	public abstract boolean isAssignableFrom(ShadowClass klass);

	public abstract boolean isInterface();

	public abstract boolean isPrimitive();

	public abstract boolean isAnnotation();

	public abstract boolean isSynthetic();

	public abstract boolean isEnum();

	public abstract String getName();

	public abstract String getCanonicalName();

	public abstract String[] getInterfaces();

	public abstract String getPackage();

	public abstract ShadowClass getSuperclass();

	@Override
	public boolean equals(Object obj) {

		if (!(obj instanceof ShadowClass)) {
			return false;
		}

		ShadowClass sClass = (ShadowClass) obj;

		if (getName().equals(sClass.getName())
				&& getShadowClassLoader().equals(sClass.getShadowClassLoader())) {
			return true;
		}

		return false;
	}
}