package ch.usi.dag.disl.staticcontext.generator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticContextGenException;
import ch.usi.dag.disl.marker.MarkedRegion;
import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.snippet.StaticContextMethod;
import ch.usi.dag.disl.staticcontext.StaticContext;
import ch.usi.dag.disl.staticcontext.Shadow;
import ch.usi.dag.disl.util.Constants;
import ch.usi.dag.disl.util.ReflectionHelper;

public class SCGenerator {

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

		private SCGenerator getOuterType() {
			return SCGenerator.this;
		}
	}

	Map<StaticInfoKey, Object> staticInfoData = new HashMap<StaticInfoKey, Object>();

	public SCGenerator(Map<Class<?>, Object> staticContextInstances,
			ClassNode classNode, MethodNode methodNode,
			Map<Snippet, List<MarkedRegion>> snippetMarkings)
			throws ReflectionException, StaticContextGenException {

		computeStaticInfo(staticContextInstances, classNode, methodNode,
				snippetMarkings);
	}
	
	private StaticInfoKey createStaticInfoKey(Snippet snippet,
			MarkedRegion markedRegion, String infoClass, String infoMethod) {
		
		String methodID = infoClass + Constants.STATIC_CONTEXT_METHOD_DELIM
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

	// Call static context for each snippet and each marked region and create
	// a static info values
	private void computeStaticInfo(
			Map<Class<?>, Object> staticContextInstances, ClassNode classNode,
			MethodNode methodNode,
			Map<Snippet, List<MarkedRegion>> snippetMarkings)
			throws ReflectionException, StaticContextGenException {

		for (Snippet snippet : snippetMarkings.keySet()) {

			for (MarkedRegion markedRegion : snippetMarkings.get(snippet)) {

				for (String stConMehodName : snippet.getCode()
						.getStaticContexts().keySet()) {

					// get static context method
					StaticContextMethod stAnMethod = snippet.getCode()
							.getStaticContexts().get(stConMehodName);

					// get static context instance
					Class<?> methodClass = stAnMethod.getReferencedClass();
					Object scInst = staticContextInstances.get(methodClass);

					// ... or create new one
					if (scInst == null) {

						scInst = ReflectionHelper.createInstance(methodClass);

						// and store for later use
						staticContextInstances.put(methodClass, scInst);
					}

					// recast context object to interface
					StaticContext scIntr = (StaticContext) scInst;

					// create static context info data
					Shadow scData = new Shadow(
							classNode, methodNode, snippet, markedRegion);

					// compute static data using context
					Object result = scIntr
							.computeStaticData(stAnMethod.getMethod(), scData);

					// store the result
					setSI(snippet, markedRegion, stConMehodName, result);
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
