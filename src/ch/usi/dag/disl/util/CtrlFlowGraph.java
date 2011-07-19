package ch.usi.dag.disl.util;

import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;

public class CtrlFlowGraph {

	public static class BasicBlock {
		public AbstractInsnNode entrance;
		public AbstractInsnNode exit;
		
		public List<BasicBlock> successors;

		public BasicBlock(AbstractInsnNode entrance, AbstractInsnNode exit) {
			this.entrance = entrance;
			this.exit = exit;
			this.successors = new LinkedList<BasicBlock>();
		}
	}

	private List<BasicBlock> nodes;
	private List<BasicBlock> connected_nodes;

	// Initialize the control flow graph.
	public CtrlFlowGraph(MethodNode method) {
		nodes = new LinkedList<BasicBlock>();
		connected_nodes = new LinkedList<BasicBlock>();

		// Generating basic blocks
		List<AbstractInsnNode> seperators = InsnListHelper.getBasicBlocks(
				method, false);
		AbstractInsnNode last = method.instructions.getLast();
		seperators.add(last);

		for (int i = 0; i < seperators.size() - 1; i++) {

			AbstractInsnNode start = seperators.get(i);
			AbstractInsnNode end = seperators.get(i + 1);

			if (end != last) {
				end = end.getPrevious();
			}

			end = InsnListHelper.skipLabels(end, false);
			nodes.add(new BasicBlock(start, end));
		}
	}

	// Get a basic block according to its entrance.
	// If not found, return null.
	public BasicBlock getBB(AbstractInsnNode entrance) {

		for (int i = 0; i < nodes.size(); i++) {
			if (nodes.get(i).entrance.equals(entrance)) {
				return nodes.get(i);
			}
		}

		return null;
	}

	// Visit a successor. 
	// If the basic block, which starts with the input 'node', 
	// is not found, return -1;
	// If the basic block has been visited, then returns 0;
	// Otherwise return 1.
	public int tryVisit(BasicBlock current, AbstractInsnNode node) {

		BasicBlock bb = getBB(node);

		if (bb == null) {
			return -1;
		}

		if (connected_nodes.contains(bb)) {
			return 0;
		}
		
		if (current != null){
			current.successors.add(bb);
		}

		connected_nodes.add(bb);
		return 1;
	}

	// Try to visit a successor. If it is visited, then regards
	// it as an exit.
	public void tryVisit(BasicBlock current, AbstractInsnNode node, AbstractInsnNode exit,
			List<AbstractInsnNode> exits) {
		if (tryVisit(current, node) == 0) {
			exits.add(exit);
		}
	}

	// Generate a control flow graph.
	// Returns a list of instruction that stands for the exit point
	// of the current visit.
	// For the first time this method is called, it will generate
	// the normal return of this method.
	// Otherwise, it will generate the join instruction between
	// the current visit and a existing visit.
	public List<AbstractInsnNode> visit(AbstractInsnNode root) {

		List<AbstractInsnNode> exits = new LinkedList<AbstractInsnNode>();
		int i = connected_nodes.size();

		if (tryVisit(null, root) <= 0) {
			return exits;
		}

		for (; i < connected_nodes.size(); i++) {
			BasicBlock current = connected_nodes.get(i);
			AbstractInsnNode exit = InsnListHelper.skipLabels(current.exit, false);

			int opcode = exit.getOpcode();

			switch (exit.getType()) {
			case AbstractInsnNode.JUMP_INSN: {
				// Covers IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ,
				// IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE,
				// IF_ACMPEQ, IF_ACMPNE, GOTO, JSR, IFNULL, and IFNONNULL.
				tryVisit(current, ((JumpInsnNode) exit).label, exit, exits);

				// goto never returns.
				if (opcode != Opcodes.GOTO) {
					tryVisit(current, exit.getNext(), exit, exits);
				}

				break;
			}

			case AbstractInsnNode.LOOKUPSWITCH_INSN: {
				// Covers LOOKUPSWITCH
				LookupSwitchInsnNode lsin = (LookupSwitchInsnNode) exit;

				for (LabelNode label : lsin.labels) {
					tryVisit(current, label, exit, exits);
				}

				tryVisit(current, lsin.dflt, exit, exits);

				break;
			}

			case AbstractInsnNode.TABLESWITCH_INSN: {
				// Covers TABLESWITCH
				TableSwitchInsnNode tsin = (TableSwitchInsnNode) exit;

				for (LabelNode label : tsin.labels) {
					tryVisit(current, label, exit, exits);
				}

				tryVisit(current, tsin.dflt, exit, exits);
				break;
			}

			default:
				tryVisit(current, exit.getNext(), exit, exits);
				break;
			}
		}
		
		return exits;
	}
}
