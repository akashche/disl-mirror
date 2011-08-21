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

public class CtrlFlowGraph {
	
	public final static int NOT_FOUND = -1;
	public final static int NEW = -2;

	final private InsnList instructions;
	final private List<TryCatchBlockNode> tryCatchBlocks;

	private List<BasicBlock> nodes;
	private List<BasicBlock> connected_nodes;

	private Set<BasicBlock> method_exits;

	private List<AbstractInsnNode> seperators;
	
	private int connected_size;

	// Initialize the control flow graph.
	public CtrlFlowGraph(InsnList instructions,
			List<TryCatchBlockNode> tryCatchBlocks) {

		this.instructions = instructions;
		this.tryCatchBlocks = tryCatchBlocks;

		nodes = new LinkedList<BasicBlock>();
		connected_nodes = new LinkedList<BasicBlock>();

		method_exits = new HashSet<BasicBlock>();

		// Generating basic blocks
		seperators = AsmHelper.getBasicBlocks(instructions, tryCatchBlocks,
				false);
		AbstractInsnNode last = instructions.getLast();
		seperators.add(last);

		for (int i = 0; i < seperators.size() - 1; i++) {

			AbstractInsnNode start = seperators.get(i);
			AbstractInsnNode end = seperators.get(i + 1);

			if (i != seperators.size() - 2) {
				end = end.getPrevious();
			}

			end = AsmHelper.skipLabels(end, false);
			nodes.add(new BasicBlock(i, start, end));
		}
		
		connected_size = 0;
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

		instr = AsmHelper.skipLabels(instr, true);

		while (instr != null) {
			if (seperators.contains(instr)) {
				break;
			}

			instr = instr.getPrevious();
		}

		for (int i = 0; i < nodes.size(); i++) {
			if (nodes.get(i).getEntrance().equals(instr)) {
				return nodes.get(i);
			}
		}

		return null;
	}

	// Visit a successor.
	// If the basic block, which starts with the input 'node',
	// is not found, return NOT_FOUND;
	// If the basic block has been visited, then returns its index;
	// Otherwise return NEW.
	public int tryVisit(BasicBlock current, AbstractInsnNode node) {

		BasicBlock bb = getBB(node);

		if (bb == null) {
			return NOT_FOUND;
		}
		
		if (connected_nodes.contains(bb)) {
			
			int index = connected_nodes.indexOf(bb);
			
			if (current != null) {
				if (index < connected_size) {
					current.getJoins().add(bb);
				}else{
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
	public void tryVisit(BasicBlock current, AbstractInsnNode node,
			AbstractInsnNode exit, List<AbstractInsnNode> joins, int size) {
		int ret = tryVisit(current, node);
		
		if (ret >= 0 && ret < size) {
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
				tryVisit(current, ((JumpInsnNode) exit).label, exit, joins,
						connected_size);

				// goto never returns.
				if (opcode != Opcodes.GOTO) {
					tryVisit(current, exit.getNext(), exit, joins,
							connected_size);
				}

				break;
			}

			case AbstractInsnNode.LOOKUPSWITCH_INSN: {
				// Covers LOOKUPSWITCH
				LookupSwitchInsnNode lsin = (LookupSwitchInsnNode) exit;

				for (LabelNode label : lsin.labels) {
					tryVisit(current, label, exit, joins, connected_size);
				}

				tryVisit(current, lsin.dflt, exit, joins, connected_size);

				break;
			}

			case AbstractInsnNode.TABLESWITCH_INSN: {
				// Covers TABLESWITCH
				TableSwitchInsnNode tsin = (TableSwitchInsnNode) exit;

				for (LabelNode label : tsin.labels) {
					tryVisit(current, label, exit, joins, connected_size);
				}

				tryVisit(current, tsin.dflt, exit, joins, connected_size);
				break;
			}

			default:
				if ((opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN)
						|| opcode == Opcodes.ATHROW) {
					method_exits.add(current);
				} else {
					tryVisit(current, exit.getNext(), exit, joins,
							connected_size);
				}

				break;
			}
		}
		
		connected_size = connected_nodes.size();

		return joins;
	}

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

	// Get last store instructions
	public Set<AbstractInsnNode> lastStore(AbstractInsnNode instr,
			AbstractInsnNode criterion) {
		Set<AbstractInsnNode> source = new HashSet<AbstractInsnNode>();

		Set<BasicBlock> bbs = new HashSet<BasicBlock>();
		Set<BasicBlock> visited = new HashSet<BasicBlock>();
		
		BasicBlock current = getBB(instr);
		visited.add(current);

		while (true) {
			
			while (instr != current.getEntrance()) {
				
				if (testStore(source, instr, criterion)) {
					break;
				}

				instr = instr.getPrevious();
			}
			
			if (instr == current.getEntrance()
					&& !testStore(source, instr, criterion)) {

				for (BasicBlock bb : current.getPredecessors()) {
					
					if (!visited.contains(bb)) {
						bbs.add(bb);
					}
				}
			}
			
			if (bbs.size() > 0) {
				current = bbs.iterator().next();
				bbs.remove(current);
				visited.add(current);
				instr = current.getExit();
			} else {
				break;
			}
		}
		
		return source;
	}
	
	public void computeDominators(AbstractInsnNode instr) {

		BasicBlock entry = getBB(instr);

		entry.getDominators().add(entry);

		for (BasicBlock bb : connected_nodes) {

			if (bb != entry) {
				bb.getDominators().addAll(connected_nodes);
			}
		}

		boolean flag;

		do {
			flag = false;

			for (BasicBlock bb : connected_nodes) {

				if (bb == entry) {
					continue;
				}

				bb.getDominators().remove(bb);

				for (BasicBlock predecessor : bb.getPredecessors()) {

					if (bb.getDominators().retainAll(
							predecessor.getDominators())) {
						flag = true;
					}
				}

				bb.getDominators().add(bb);
			}
		} while (flag);
	}

	public void build() {
		visit(instructions.getFirst());
		computeDominators(instructions.getFirst());

		for (int i = tryCatchBlocks.size() - 1; i >= 0; i--) {
			visit(tryCatchBlocks.get(i).handler);
			computeDominators(instructions.getFirst());
		}
		
		for (BasicBlock bb : connected_nodes) {
			for (BasicBlock successor : bb.getSuccessors()) {
				if (bb.getDominators().contains(successor)) {
					successor.setLoop(true);
				}
			}
		}
	}

}
