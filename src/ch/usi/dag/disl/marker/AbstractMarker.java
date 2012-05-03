package ch.usi.dag.disl.marker;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.exception.MarkerException;
import ch.usi.dag.disl.snippet.Shadow;
import ch.usi.dag.disl.snippet.Snippet;

public abstract class AbstractMarker implements Marker {

	// holder for marked region
	public static class MarkedRegion {

		private AbstractInsnNode start;
		private List<AbstractInsnNode> ends;
		
		private AbstractInsnNode afterThrowStart;
		private AbstractInsnNode afterThrowEnd;

		public AbstractInsnNode getStart() {
			return start;
		}

		public List<AbstractInsnNode> getEnds() {
			return ends;
		}
		
		public AbstractInsnNode getAfterThrowStart() {
			return afterThrowStart;
		}

		public AbstractInsnNode getAfterThrowEnd() {
			return afterThrowEnd;
		}

		public void setStart(AbstractInsnNode start) {
			this.start = start;
		}
		
		public void setAfterThrowStart(AbstractInsnNode afterThrowStart) {
			this.afterThrowStart = afterThrowStart;
		}

		public void setAfterThrowEnd(AbstractInsnNode afterThrowEnd) {
			this.afterThrowEnd = afterThrowEnd;
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
		
		public boolean valid() {
			return start != null && ends != null
					&& afterThrowStart != null && afterThrowEnd != null;
		}
		
		public void computeAfterThrow(MethodNode methodNode) {
			
			// set start
			afterThrowStart = start;
			
			// get end that is the latest in the method instructions 
			Set<AbstractInsnNode> endsSet = new HashSet<AbstractInsnNode>(ends);

			AbstractInsnNode instr = methodNode.instructions.getLast();
			
			while(instr != null) {
				
				if(endsSet.contains(instr)) {
					afterThrowEnd = instr;
					break;
				}
				
				instr.getPrevious();
			}
		}
		
		public void skipBranchesAtTheEnds() {
			
			// TODO ! markers
		}
	}

	@Override
	public List<Shadow> mark(ClassNode classNode, MethodNode methodNode,
			Snippet snippet) throws MarkerException {
		
		// use simplified interface
		List<MarkedRegion> regions = mark(methodNode);
		
		List<Shadow> result = new LinkedList<Shadow>();
		
		// convert marked regions to shadows
		for (MarkedRegion mr : regions) {

			if(! mr.valid()) {
				throw new MarkerException("Marker " + this.getClass()
						+ " produced invalid MarkedRegion (some MarkedRegion" +
						" fields where not set)");
			}
			
			result.add(new Shadow(classNode, methodNode, snippet,
					mr.getStart(), mr.getEnds(), mr.getAfterThrowStart(),
					mr.getAfterThrowEnd()));
		}
		
		return result;
	}
	
	public abstract List<MarkedRegion> mark(MethodNode methodNode);
}
