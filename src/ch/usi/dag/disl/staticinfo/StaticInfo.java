package ch.usi.dag.disl.staticinfo;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.exception.DiSLFatalException;
import ch.usi.dag.disl.exception.StaticAnalysisException;
import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.snippet.marker.MarkedRegion;
import ch.usi.dag.disl.staticinfo.analysis.StaticAnalysis;
import ch.usi.dag.disl.staticinfo.analysis.StaticAnalysisInfo;
import ch.usi.dag.disl.util.Constants;

public class StaticInfo {

	// static analysis info setter method
	private static Method methodSetSAI;
	static { // static init for methodSetSAI
		try {
			methodSetSAI = 
				StaticAnalysis.class.getMethod("setStaticAnalysisInfo", StaticAnalysisInfo.class);
		} catch (Exception e) {
			throw new DiSLFatalException("setStaticAnalysisInfo in " + 
					StaticAnalysis.class.getName() + " has been renamed."); 
		}
	}
	
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
	
	public StaticInfo(Map<Class<?>, Object> staticAnalysisInstances,
			ClassNode classNode, MethodNode methodNode,
			Map<Snippet, List<MarkedRegion>> snippetMarkings)
			throws StaticAnalysisException {

		computeStaticInfo(staticAnalysisInstances, classNode, methodNode,
				snippetMarkings);
	}
	
	public Object getSI(Snippet snippet, MarkedRegion markedRegion,
			String infoClass, String infoMethod) {

		String methodID = infoClass + Constants.STATIC_ANALYSIS_METHOD_DELIM
				+ infoMethod;

		return staticInfoData.get(new StaticInfoKey(snippet, markedRegion,
				methodID));
	}

	// Call static analysis for each snippet and each marked region and create
	// a static info values
	private void computeStaticInfo(
			Map<Class<?>, Object> staticAnalysisInstances, ClassNode classNode,
			MethodNode methodNode,
			Map<Snippet, List<MarkedRegion>> snippetMarkings)
			throws StaticAnalysisException {
		
		for(Snippet snippet : snippetMarkings.keySet()) {
			
			for(MarkedRegion markedRegion : snippetMarkings.get(snippet)) {
				
				for(String stAnMehodName : snippet.getStaticAnalyses().keySet()) {

					try {
					
						// create static analysis info data
						StaticAnalysisInfo saInfo = new StaticAnalysisInfo(
								classNode, methodNode, snippet,
								snippetMarkings.get(snippet), markedRegion);

						// get static analysis method
						Method stAnMethod = 
							snippet.getStaticAnalyses().get(stAnMehodName);

						// get static analysis instance
						Class<?> methodClass = 
							stAnMethod.getDeclaringClass();
						Object saInst = staticAnalysisInstances.get(methodClass);

						// ... or create new one
						if (saInst == null) {
							
							saInst = methodClass.newInstance();
							
							// and store for later use
							staticAnalysisInstances.put(methodClass, saInst);
						}
						
						// invoke static analysis info setter method
						// returns cached result or null
						Object result = methodSetSAI.invoke(saInst, saInfo);
						
						// if cache wasn't hit, invoke static analysis method
						if(result == null) {
							result = stAnMethod.invoke(saInst);
						}
						
						// store the result
						setSI(snippet, markedRegion, stAnMehodName, result);
						
					} catch (Exception e) {
						throw new StaticAnalysisException(
								"Invocation of static analysis method " +
								stAnMehodName + " failed", e);
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
