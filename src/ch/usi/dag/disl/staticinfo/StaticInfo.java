package ch.usi.dag.disl.staticinfo;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.dislclass.snippet.Snippet;
import ch.usi.dag.disl.dislclass.snippet.marker.MarkedRegion;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticInfoException;
import ch.usi.dag.disl.staticinfo.analysis.StaticAnalysis;
import ch.usi.dag.disl.staticinfo.analysis.StaticAnalysisData;
import ch.usi.dag.disl.util.Constants;
import ch.usi.dag.disl.util.ReflectionHelper;

public class StaticInfo {

	class StaticInfoKey {

		private Snippet snippet;
		private MarkedRegion markedRegion;
		private String methodID;

		public StaticInfoKey(Snippet snippet, MarkedRegion markedRegion,
				String methodID) {
			super();
			this.snippet = snippet;
			this.markedRegion = markedRegion;
			this.methodID = methodID;
		}

		@Override
		public int hashCode() {

			final int prime = 31;
			int result = 1;

			result = prime * result + getOuterType().hashCode();

			result = prime * result
					+ ((markedRegion == null) ? 0 : markedRegion.hashCode());

			result = prime * result
					+ ((methodID == null) ? 0 : methodID.hashCode());

			result = prime * result
					+ ((snippet == null) ? 0 : snippet.hashCode());

			return result;
		}

		@Override
		public boolean equals(Object obj) {

			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			StaticInfoKey other = (StaticInfoKey) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;

			if (markedRegion == null) {
				if (other.markedRegion != null) {
					return false;
				}
			} else if (!markedRegion.equals(other.markedRegion)) {
				return false;
			}

			if (methodID == null) {
				if (other.methodID != null) {
					return false;
				}
			} else if (!methodID.equals(other.methodID)) {
				return false;
			}

			if (snippet == null) {
				if (other.snippet != null) {
					return false;
				}
			} else if (!snippet.equals(other.snippet)) {
				return false;
			}

			return true;
		}

		private StaticInfo getOuterType() {
			return StaticInfo.this;
		}
	}

	Map<StaticInfoKey, Object> staticInfoData = new HashMap<StaticInfoKey, Object>();

	public StaticInfo(Map<Class<?>, Object> staticAnalysisInstances,
			ClassNode classNode, MethodNode methodNode,
			Map<Snippet, List<MarkedRegion>> snippetMarkings)
			throws ReflectionException, StaticInfoException {

		computeStaticInfo(staticAnalysisInstances, classNode, methodNode,
				snippetMarkings);
	}
	
	private StaticInfoKey createStaticInfoKey(Snippet snippet,
			MarkedRegion markedRegion, String infoClass, String infoMethod) {
		
		String methodID = infoClass + Constants.STATIC_ANALYSIS_METHOD_DELIM
				+ infoMethod;
		
		return new StaticInfoKey(snippet, markedRegion, methodID);
	}
	
	public boolean contains(Snippet snippet, MarkedRegion markedRegion,
			String infoClass, String infoMethod) {
		
		StaticInfoKey sik = 
			createStaticInfoKey(snippet, markedRegion, infoClass, infoMethod);
		
		return staticInfoData.containsKey(sik);
	}

	public Object get(Snippet snippet, MarkedRegion markedRegion,
			String infoClass, String infoMethod) {

		StaticInfoKey sik = 
			createStaticInfoKey(snippet, markedRegion, infoClass, infoMethod);
		
		return staticInfoData.get(sik);
	}

	// Call static analysis for each snippet and each marked region and create
	// a static info values
	private void computeStaticInfo(
			Map<Class<?>, Object> staticAnalysisInstances, ClassNode classNode,
			MethodNode methodNode,
			Map<Snippet, List<MarkedRegion>> snippetMarkings)
			throws ReflectionException, StaticInfoException {

		for (Snippet snippet : snippetMarkings.keySet()) {

			for (MarkedRegion markedRegion : snippetMarkings.get(snippet)) {

				for (String stAnMehodName : snippet.getCode()
						.getStaticAnalyses().keySet()) {

					// get static analysis method
					Method stAnMethod = snippet.getCode().getStaticAnalyses()
							.get(stAnMehodName);

					// get static analysis instance
					Class<?> methodClass = stAnMethod.getDeclaringClass();
					Object saInst = staticAnalysisInstances.get(methodClass);

					// ... or create new one
					if (saInst == null) {

						saInst = ReflectionHelper.createInstance(methodClass);

						// and store for later use
						staticAnalysisInstances.put(methodClass, saInst);
					}

					// recast analysis object to interface
					StaticAnalysis saIntr = (StaticAnalysis) saInst;

					// create static analysis info data
					StaticAnalysisData saData = new StaticAnalysisData(
							classNode, methodNode, snippet,
							snippetMarkings.get(snippet), markedRegion);

					// compute static data using analysis
					Object result = saIntr
							.computeStaticData(stAnMethod, saData);

					// store the result
					setSI(snippet, markedRegion, stAnMehodName, result);
				}
			}
		}
	}

	private void setSI(Snippet snippet, MarkedRegion markedRegion,
			String methodID, Object value) {

		staticInfoData.put(new StaticInfoKey(snippet, markedRegion, methodID),
				value);
	}
}
