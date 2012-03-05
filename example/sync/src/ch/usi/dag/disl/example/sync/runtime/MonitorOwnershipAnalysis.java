package ch.usi.dag.disl.example.sync.runtime;

import ch.usi.dag.disl.example.sync.runtime.AbstractShadowHeapAnalysis;

/**
 * @author Andreas Sewe
 */
public class MonitorOwnershipAnalysis extends AbstractShadowHeapAnalysis<MonitorOwnershipShadowObject> {

	/**
	 * Convenience method that avoids casting to <code>MonitorOwnershipAnalysis</code> in the snippet.
	 */
	public static MonitorOwnershipAnalysis getInstance() {
		return (MonitorOwnershipAnalysis) AbstractShadowHeapAnalysis.getInstance();
	}

	@Override
	public MonitorOwnershipShadowObject createShadowObject(Object object, String allocationSite) {
		return new MonitorOwnershipShadowObject(object, allocationSite);
	}

	public void onMonitorEntry(Object object) {
		if (isSaveToProcessEvent())
			if (object != null)
				getShadowObject(object).onMonitorEntry();
	}

	public void onMonitorExit(Object object) {
		if (isSaveToProcessEvent())
			if (object != null)
				getShadowObject(object).onMonitorExit();
	}
}
