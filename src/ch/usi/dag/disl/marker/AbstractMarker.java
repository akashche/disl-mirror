package ch.usi.dag.disl.marker;

import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.snippet.Shadow;
import ch.usi.dag.disl.snippet.Snippet;

public abstract class AbstractMarker implements Marker {

	// holder for marked region
	public static class MarkedRegion {

		private AbstractInsnNode start;
		private List<AbstractInsnNode> ends;

		public AbstractInsnNode getStart() {
			return start;
		}

		public List<AbstractInsnNode> getEnds() {
			return ends;
		}

		public void setStart(AbstractInsnNode start) {
			this.start = start;
		}

		public MarkedRegion(AbstractInsnNode start) {
			this.start = start;
			this.ends = new LinkedList<AbstractInsnNode>();
		}

		public MarkedRegion(AbstractInsnNode start, AbstractInsnNode end) {
			this.start = start;
			this.ends = new LinkedList<AbstractInsnNode>();
			this.ends.add(end);
		}

		public MarkedRegion(AbstractInsnNode start,	List<AbstractInsnNode> ends) {
			this.start = start;
			this.ends = ends;
		}

		public void addExitPoint(AbstractInsnNode exitpoint) {
			this.ends.add(exitpoint);
		}
	}

	@Override
	public List<Shadow> mark(ClassNode classNode, MethodNode methodNode,
			Snippet snippet) {
		
		// use simplified interface
		List<MarkedRegion> regions = mark(methodNode);
		
		List<Shadow> result = new LinkedList<Shadow>();
		
		// convert marked regions to shadows
		for (MarkedRegion mr : regions) {

			result.add(new Shadow(classNode, methodNode, snippet,
					mr.getStart(), mr.getEnds()));
		}
		
		return result;
	}
	
	public abstract List<MarkedRegion> mark(MethodNode methodNode);
}
