package ch.usi.dag.disl.staticinfo;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.exception.AnalysisException;
import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.snippet.marker.MarkedRegion;
import ch.usi.dag.disl.staticinfo.analysis.AnalysisInfo;
import ch.usi.dag.disl.util.Constants;

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
	
	Map<StaticInfoKey, Object> staticInfoData = 
		new HashMap<StaticInfoKey, Object>();
	
	public StaticInfo(ClassNode classNode,
			MethodNode methodNode, Map<Snippet,
			List<MarkedRegion>> snippetMarkings) throws AnalysisException {
		
		computeStaticInfo(classNode, methodNode, snippetMarkings);
	}
	
	public Object getSI(Snippet snippet, MarkedRegion markedRegion,
			String infoClass, String infoMethod) {

		String methodID = infoClass + Constants.ANALYSIS_METHOD_DELIM
				+ infoMethod;

		return staticInfoData.get(new StaticInfoKey(snippet, markedRegion,
				methodID));
	}

	// Call analysis for each snippet and each marked region and create
	// a static info values
	private void computeStaticInfo(ClassNode classNode,
			MethodNode methodNode, Map<Snippet,
			List<MarkedRegion>> snippetMarkings) throws AnalysisException {
		
		for(Snippet snippet : snippetMarkings.keySet()) {
			
			for(MarkedRegion markedRegion : snippetMarkings.get(snippet)) {
				
				for(String analysisMehodName : snippet.getAnalyses().keySet()) {

					// create analysis info data
					AnalysisInfo ai = new AnalysisInfo(classNode, methodNode,
							snippet, snippetMarkings.get(snippet),
							markedRegion);
					
					// get analysis method
					Method analysisMethod = 
						snippet.getAnalyses().get(analysisMehodName);
					
					try {
						// invoke analysis method
						Object result = analysisMethod.invoke(null, ai);
						
						// store the result
						setSI(snippet, markedRegion, analysisMehodName, result);
						
					} catch (Exception e) {
						throw new AnalysisException(
								"Cannot invoke analysis method", e);
					}
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
