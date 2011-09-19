package ch.usi.dag.disl.util.cfg;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

import ch.usi.dag.disl.util.AsmHelper;
import ch.usi.dag.disl.util.BasicBlockCalc;

public class CtrlFlowGraph {

	public final static int NOT_FOUND = -1;
	public final static int NEW = -2;

	// basic blocks of a method
	private List<BasicBlock> nodes;

	// a basic block is marked as connected after visited
	private List<BasicBlock> connected_nodes;

	// size of connected basic blocks since last visit
	private int connected_size;

	// basic blocks that ends with a 'return' or 'athrow'
	private Set<BasicBlock> method_exits;

	// Initialize the control flow graph.
	public CtrlFlowGraph(InsnList instructions,
			List<TryCatchBlockNode> tryCatchBlocks) {

		nodes = new LinkedList<BasicBlock>();
		connected_nodes = new LinkedList<BasicBlock>();
		connected_size = 0;

		method_exits = new HashSet<BasicBlock>();

		// Generating basic blocks
		List<AbstractInsnNode> seperators = BasicBlockCalc.getAll(instructions,
				tryCatchBlocks, false);
		AbstractInsnNode last = instructions.getLast();
		seperators.add(last);

		for (int i = 0; i < seperators.size() - 1; i++) {

			AbstractInsnNode start = seperators.get(i);
			AbstractInsnNode end = seperators.get(i + 1);

			if (i != seperators.size() - 2) {
				end = end.getPrevious();
			}

			end = AsmHelper.skipVirualInsns(end, false);
			nodes.add(new BasicBlock(i, start, end));
		}
	}

	// Initialize the control flow graph.
	public CtrlFlowGraph(MethodNode method) {
		this(method.instructions, method.tryCatchBlocks);
	}

	public List<BasicBlock> getNodes() {
		return nodes;
	}

	// Return the index of basic block that contains the input instruction.
	// If not found, return NOT_FOUND.
	public int getIndex(AbstractInsnNode instr) {

		BasicBlock bb = getBB(instr);

		if (bb == null) {
			return NOT_FOUND;
		} else {
			return bb.getIndex();
		}
	}

	// Return a basic block that contains the input instruction.
	// If not found, return null.
	public BasicBlock getBB(AbstractInsnNode instr) {

		instr = AsmHelper.skipVirualInsns(instr, true);

		while (instr != null) {

			for (int i = 0; i < nodes.size(); i++) {
				if (nodes.get(i).getEntrance().equals(instr)) {
					return nodes.get(i);
				}
			}

			instr = instr.getPrevious();
		}

		return null;
	}

	// Visit a successor.
	// If the basic block, which starts with the input 'node',
	// is not found, return NOT_FOUND;
	// If the basic block has been visited, then returns its index;
	// Otherwise return NEW.
	private int tryVisit(BasicBlock current, AbstractInsnNode node) {

		BasicBlock bb = getBB(node);

		if (bb == null) {
			return NOT_FOUND;
		}

		if (connected_nodes.contains(bb)) {

			int index = connected_nodes.indexOf(bb);

			if (current != null) {
				if (index < connected_size) {
					current.getJoins().add(bb);
				} else {
					current.getSuccessors().add(bb);
					bb.getPredecessors().add(current);
				}
			}

			return index;
		}

		if (current != null) {
			current.getSuccessors().add(bb);
			bb.getPredecessors().add(current);
		}

		connected_nodes.add(bb);
		return NEW;
	}

	// Try to visit a successor. If it is visited last build, then regards
	// it as an exit.
	private void tryVisit(BasicBlock current, AbstractInsnNode node,
			AbstractInsnNode exit, List<AbstractInsnNode> joins) {
		int ret = tryVisit(current, node);

		if (ret >= 0 && ret < connected_size) {
			joins.add(exit);
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

		List<AbstractInsnNode> joins = new LinkedList<AbstractInsnNode>();

		if (tryVisit(null, root) == NOT_FOUND) {
			return joins;
		}

		for (int i = connected_size; i < connected_nodes.size(); i++) {

			BasicBlock current = connected_nodes.get(i);
			AbstractInsnNode exit = current.getExit();

			int opcode = exit.getOpcode();

			switch (exit.getType()) {
			case AbstractInsnNode.JUMP_INSN: {
				// Covers IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ,
				// IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE,
				// IF_ACMPEQ, IF_ACMPNE, GOTO, JSR, IFNULL, and IFNONNULL.
				tryVisit(current, ((JumpInsnNode) exit).label, exit, joins);

				// goto never returns.
				if (opcode != Opcodes.GOTO) {
					tryVisit(current, exit.getNext(), exit, joins);
				}

				break;
			}

			case AbstractInsnNode.LOOKUPSWITCH_INSN: {
				// Covers LOOKUPSWITCH
				LookupSwitchInsnNode lsin = (LookupSwitchInsnNode) exit;

				for (LabelNode label : lsin.labels) {
					tryVisit(current, label, exit, joins);
				}

				tryVisit(current, lsin.dflt, exit, joins);

				break;
			}

			case AbstractInsnNode.TABLESWITCH_INSN: {
				// Covers TABLESWITCH
				TableSwitchInsnNode tsin = (TableSwitchInsnNode) exit;

				for (LabelNode label : tsin.labels) {
					tryVisit(current, label, exit, joins);
				}

				tryVisit(current, tsin.dflt, exit, joins);
				break;
			}

			default:
				if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN)
						|| opcode == Opcodes.ATHROW) {
					method_exits.add(current);
				} else {
					tryVisit(current, exit.getNext(), exit, joins);
				}

				break;
			}
		}

		connected_size = connected_nodes.size();

		return joins;
	}

	// test if the current instruction rewrites a field
	// add to 'source' if it does rewrite
	private boolean testStore(Set<AbstractInsnNode> source,
			AbstractInsnNode instr, String owner, String name) {

		if (instr.getOpcode() == Opcodes.PUTFIELD
				|| instr.getOpcode() == Opcodes.PUTSTATIC) {

			FieldInsnNode fin = (FieldInsnNode) instr;

			if (owner.equals(fin.owner) && name.equals(fin.name)) {
				source.add(instr);
				return true;
			}
		}

		return false;
	}

	// test if the current instruction rewrites a local slot
	// add to 'source' if it does rewrite, or if it is an 'iinc' instruction,
	// which updates the local slot, but still relies on the value of last store
	private boolean testStore(Set<AbstractInsnNode> source,
			AbstractInsnNode instr, int var) {

		switch (instr.getOpcode()) {

		case Opcodes.ISTORE:
		case Opcodes.LSTORE:
		case Opcodes.FSTORE:
		case Opcodes.DSTORE:
		case Opcodes.ASTORE:

			if (((VarInsnNode) instr).var == var) {
				source.add(instr);
				return true;
			}
		case Opcodes.IINC:

			if (((IincInsnNode) instr).var == var) {
				source.add(instr);
				return false;
			}
		default:
			return false;
		}
	}

	// test if the current instruction rewrites a field or a local slot
	private boolean testStore(Set<AbstractInsnNode> source,
			AbstractInsnNode instr, AbstractInsnNode criterion) {

		if (criterion instanceof FieldInsnNode) {
			FieldInsnNode fin = (FieldInsnNode) criterion;
			return testStore(source, instr, fin.owner, fin.name);
		} else if (criterion instanceof VarInsnNode) {
			return testStore(source, instr, ((VarInsnNode) criterion).var);
		} else {
			return false;
		}
	}

	// Get last store instructions before 'instr'
	// 'criterion' is either a field instruction that indicates the field node,
	// or a load/store instruction that indicates the local slot number.
	public Set<AbstractInsnNode> lastStore(AbstractInsnNode instr,
			AbstractInsnNode criterion) {

		Set<AbstractInsnNode> source = new HashSet<AbstractInsnNode>();

		Set<BasicBlock> unvisited = new HashSet<BasicBlock>();
		Set<BasicBlock> visited = new HashSet<BasicBlock>();

		BasicBlock current = getBB(instr);

		// marked the basic block that contains 'instr' as visited when
		// it is going to be fully visited in the loop
		if (instr.equals(current.getExit())) {
			visited.add(current);
		}

		while (true) {

			// whether last store is found
			boolean fLastStore = false;

			// backward searching
			while (instr != current.getEntrance()) {

				fLastStore = testStore(source, instr, criterion);

				if (fLastStore) {
					break;
				}

				instr = instr.getPrevious();
			}

			// visit predecessors when last store not found
			if (!fLastStore) {

				for (BasicBlock bb : current.getPredecessors()) {

					if (!visited.contains(bb)) {
						unvisited.add(bb);
					}
				}
			}

			// update current basic block and the iterator
			if (unvisited.size() > 0) {

				current = unvisited.iterator().next();
				unvisited.remove(current);
				visited.add(current);
				instr = current.getExit();
			} else {
				break;
			}
		}

		return source;
	}

	// compute dominators
	private void computeDominators(AbstractInsnNode instr) {

		// dominators of the first basic block contains only itself
		BasicBlock entry = getBB(instr);
		entry.getDominators().add(entry);

		// initialization
		for (BasicBlock bb : connected_nodes) {

			if (bb != entry) {
				bb.getDominators().addAll(connected_nodes);
			}
		}

		// whether the dominators of any basic block is changed
		boolean fChanged;

		// loop until no more changes
		do {
			fChanged = false;

			for (BasicBlock bb : connected_nodes) {

				if (bb == entry) {
					continue;
				}

				bb.getDominators().remove(bb);

				// update the dominators of current basic block,
				// contains only the dominators of its predecessors
				for (BasicBlock predecessor : bb.getPredecessors()) {

					if (bb.getDominators().retainAll(
							predecessor.getDominators())) {
						fChanged = true;
					}
				}

				bb.getDominators().add(bb);
			}
		} while (fChanged);
	}

	// build up the control flow graph of 'method'.
	// NOTE that the function will compute the dominators.
	public static CtrlFlowGraph build(MethodNode method) {
		CtrlFlowGraph cfg = new CtrlFlowGraph(method);

		cfg.visit(method.instructions.getFirst());
		cfg.computeDominators(method.instructions.getFirst());

		for (int i = method.tryCatchBlocks.size() - 1; i >= 0; i--) {
			cfg.visit(method.tryCatchBlocks.get(i).handler);
			cfg.computeDominators(method.instructions.getFirst());
		}

		for (BasicBlock bb : cfg.connected_nodes) {
			for (BasicBlock successor : bb.getSuccessors()) {
				if (bb.getDominators().contains(successor)) {
					successor.setLoop(true);
				}
			}
		}

		return cfg;
	}

}
