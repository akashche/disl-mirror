package ch.usi.dag.disl.processor.generator;

import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.tree.AbstractInsnNode;

import ch.usi.dag.disl.dislclass.snippet.Snippet;
import ch.usi.dag.disl.dislclass.snippet.marker.MarkedRegion;

public class PIResolver {

	private Map<ResolverKey, ProcInstance> piStore = 
		new HashMap<PIResolver.ResolverKey, ProcInstance>();
	
	private class ResolverKey {
		
		private Snippet snippet;
		private MarkedRegion markedRegion;
		private AbstractInsnNode instr;
		
		public ResolverKey(Snippet snippet, MarkedRegion markedRegion,
				AbstractInsnNode instr) {
			super();
			this.snippet = snippet;
			this.markedRegion = markedRegion;
			this.instr = instr;
		}

		@Override
		public int hashCode() {

			final int prime = 31;
			int result = 1;
			
			result = prime * result + getOuterType().hashCode();
			
			result = prime * result + ((instr == null) ? 0 : instr.hashCode());
			
			result = prime * result
					+ ((markedRegion == null) ? 0 : markedRegion.hashCode());
			
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
			
			ResolverKey other = (ResolverKey) obj;
			
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			
			if (instr == null) {
				if (other.instr != null)
					return false;
			} else if (!instr.equals(other.instr))
				return false;
			
			if (markedRegion == null) {
				if (other.markedRegion != null)
					return false;
			} else if (!markedRegion.equals(other.markedRegion))
				return false;
			
			if (snippet == null) {
				if (other.snippet != null)
					return false;
			} else if (!snippet.equals(other.snippet))
				return false;
			
			return true;
		}

		private PIResolver getOuterType() {
			return PIResolver.this;
		}
	}
	
	public ProcInstance get(Snippet snippet, MarkedRegion markedRegion,
			AbstractInsnNode instr) {

		ResolverKey key = new ResolverKey(snippet, markedRegion, instr);
		
		return piStore.get(key);
	}

	public void set(Snippet snippet, MarkedRegion markedRegion,
			AbstractInsnNode instr, ProcInstance processorInstance) {

		ResolverKey key = new ResolverKey(snippet, markedRegion, instr);
		
		piStore.put(key, processorInstance);
	}
}
