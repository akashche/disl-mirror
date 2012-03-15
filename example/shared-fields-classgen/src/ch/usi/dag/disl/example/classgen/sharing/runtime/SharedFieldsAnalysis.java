package ch.usi.dag.disl.example.classgen.sharing.runtime;

import java.lang.ref.WeakReference;

import ch.usi.dag.disl.example.classgen.ShadowClassDefiner;
import ch.usi.dag.disl.example.classgen.shadowheap.runtime.AbstractShadowHeapAnalysis;

public class SharedFieldsAnalysis extends AbstractShadowHeapAnalysis<SharedFieldsShadowObject> {

	final ShadowClassDefiner<SharedFieldsShadowObject> shadowClassDefiner;

	public SharedFieldsAnalysis() {
			shadowClassDefiner = new ShadowClassDefiner<SharedFieldsShadowObject>(SharedFieldsShadowObject.class);
	}
	
	/**
	 * Convenience method that avoids casting to <code>SharedFieldsAnalysis</code> in the snippet.
	 */
	public static SharedFieldsAnalysis getInstance() {
		return (SharedFieldsAnalysis) AbstractShadowHeapAnalysis.getInstance();
	}

	@Override
	public SharedFieldsShadowObject createShadowObject(Object object, String allocationSite) {
		try {
			final Thread allocatingThread = allocationSite == AbstractShadowHeapAnalysis.UNKNOWN_ALLOCATION_SITE ?
					null : Thread.currentThread();

			SharedFieldsShadowObject shadowObject = shadowClassDefiner.getShadowClass(object.getClass()).newInstance();

			shadowObject.className = object.getClass().getName();
			shadowObject.allocationSite = allocationSite;
			shadowObject.allocatingThread = new WeakReference<Thread>(allocatingThread);

			return shadowObject;
		} catch (InstantiationException e) {
			throw new AssertionError("Should not happen");
		} catch (IllegalAccessException e) {
			throw new AssertionError("Should not happen");
		}
	}


	public void onFieldRead(Object object, Class<?> owner, String fieldId) {
		if (isSaveToProcessEvent())
			if (object != null)
				getShadowObject(object).onFieldRead(owner, fieldId);
	}

	public void onFieldWrite(Object object, Class<?> owner, String fieldId) {
		if (isSaveToProcessEvent())
			if (object != null)
				getShadowObject(object).onFieldWrite(owner, fieldId);
	}

// TODO Class generation doesn't currently handle the pseudo-fields of arrays.

	public void onArrayLength(Object array) {
//		if (isSaveToProcessEvent())
//			if (array != null)
//				getShadowObject(array).onFieldRead(array.getClass(), SharedFieldsShadowObject.ARRAYLENGTH_PSEUDO_FIELD_ID);
	}

	public void onArrayRead(Object array) {
//		if (isSaveToProcessEvent())
//			if (array != null)
//				getShadowObject(array).onFieldRead(array.getClass(), SharedFieldsShadowObject.COMPONENTS_PSEUDO_FIELD_ID);
	}

	public void onArrayWrite(Object array) {
//		if (isSaveToProcessEvent())
//			if (array != null)
//				getShadowObject(array).onFieldWrite(array.getClass(), SharedFieldsShadowObject.COMPONENTS_PSEUDO_FIELD_ID);
	}
}
