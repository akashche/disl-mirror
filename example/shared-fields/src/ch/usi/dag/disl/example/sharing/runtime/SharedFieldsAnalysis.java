package ch.usi.dag.disl.example.sharing.runtime;

import ch.usi.dag.disl.example.shadowheap.runtime.AbstractShadowHeapAnalysis;

public class SharedFieldsAnalysis extends AbstractShadowHeapAnalysis<SharedFieldsShadowObject> {

	/**
	 * Convenience method that avoids casting to <code>SharedFieldsAnalysis</code> in the snippet.
	 */
	public static SharedFieldsAnalysis getInstance() {
		return (SharedFieldsAnalysis) AbstractShadowHeapAnalysis.getInstance();
	}

	@Override
	public SharedFieldsShadowObject createShadowObject(Object object, String allocationSite) {
		final Thread allocatingThread = allocationSite == AbstractShadowHeapAnalysis.UNKNOWN_ALLOCATION_SITE ?
				null : Thread.currentThread();
		return new SharedFieldsShadowObject(object, allocationSite, allocatingThread);
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

	public void onArrayLength(Object array) {
		if (isSaveToProcessEvent())
			if (array != null)
				getShadowObject(array).onFieldRead(array.getClass(), SharedFieldsShadowObject.ARRAYLENGTH_PSEUDO_FIELD_ID);
	}

	public void onArrayRead(Object array) {
		if (isSaveToProcessEvent())
			if (array != null)
				getShadowObject(array).onFieldRead(array.getClass(), SharedFieldsShadowObject.COMPONENTS_PSEUDO_FIELD_ID);
	}

	public void onArrayWrite(Object array) {
		if (isSaveToProcessEvent())
			if (array != null)
				getShadowObject(array).onFieldWrite(array.getClass(), SharedFieldsShadowObject.COMPONENTS_PSEUDO_FIELD_ID);
	}
}
