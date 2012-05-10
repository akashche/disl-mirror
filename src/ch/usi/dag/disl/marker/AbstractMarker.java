package ch.usi.dag.disl.marker;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import ch.usi.dag.disl.exception.MarkerException;
import ch.usi.dag.disl.snippet.Shadow;
import ch.usi.dag.disl.snippet.Shadow.WeavingRegion;
import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.util.AsmHelper;

public abstract class AbstractMarker implements Marker {

	// holder for marked region
	public static class MarkedRegion {

		private AbstractInsnNode start;
		private List<AbstractInsnNode> ends;
		
		private WeavingRegion weavingRegion;

		public AbstractInsnNode getStart() {
			return start;
		}

		public void setStart(AbstractInsnNode start) {
			this.start = start;
		}
		
		public List<AbstractInsnNode> getEnds() {
			return ends;
		}
		
		public void addExitPoint(AbstractInsnNode exitpoint) {
			this.ends.add(exitpoint);
		}
		
		public WeavingRegion getWeavingRegion() {
			return weavingRegion;
		}

		public void setWeavingRegion(WeavingRegion weavingRegion) {
			this.weavingRegion = weavingRegion;
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
		
		public MarkedRegion(AbstractInsnNode start,
				List<AbstractInsnNode> ends, WeavingRegion weavingRegion) {
			super();
			this.start = start;
			this.ends = ends;
			this.weavingRegion = weavingRegion;
		}

		public boolean valid() {
			return start != null && ends != null && weavingRegion != null;
		}

		public WeavingRegion computeDefaultWeavingRegion(MethodNode methodNode) {

			// skip branch instruction at the end

			AbstractInsnNode wstart = start;
			// can be null - see WeavingRegion for details
			List<AbstractInsnNode> wends = null;

			if (!ends.contains(wstart)) {

				wends = new LinkedList<AbstractInsnNode>();

				for (AbstractInsnNode instr : ends) {

					if (AsmHelper.isBranch(instr)) {
						wends.add(instr.getPrevious());
					} else {
						wends.add(instr);
					}
				}
			}
			
			// compute after throwing region
			
			// set start
			AbstractInsnNode afterThrowStart = start;
			AbstractInsnNode afterThrowEnd = null;
			
			// get end that is the latest in the method instructions 
			Set<AbstractInsnNode> endsSet = new HashSet<AbstractInsnNode>(ends);

			// get end that is the latest in the method instructions
			AbstractInsnNode instr = methodNode.instructions.getLast();
			
			while(instr != null) {
				
				if(endsSet.contains(instr)) {
					afterThrowEnd = instr;
					break;
				}
				
				instr = instr.getPrevious();
			}

			// skip the label nodes which are the end of try-catch blocks
			if (afterThrowEnd instanceof LabelNode) {

				Set<AbstractInsnNode> tcb_ends = new HashSet<AbstractInsnNode>();

				for (TryCatchBlockNode tcb : methodNode.tryCatchBlocks) {
					tcb_ends.add(tcb.end);
				}

				while (tcb_ends.contains(afterThrowEnd)) {
					afterThrowEnd = afterThrowEnd.getPrevious();
				}
			}
			
			return new WeavingRegion(wstart, wends, afterThrowStart,
					afterThrowEnd);
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
					mr.getStart(), mr.getEnds(), mr.getWeavingRegion()));
		}
		
		return result;
	}
	
	public abstract List<MarkedRegion> mark(MethodNode methodNode);
}
