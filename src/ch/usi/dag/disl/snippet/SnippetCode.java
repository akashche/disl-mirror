package ch.usi.dag.disl.snippet;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import ch.usi.dag.disl.snippet.syntheticlocal.SyntheticLocalVar;
import ch.usi.dag.disl.util.InsnListHelper;

public class SnippetCode {

	private InsnList instructions;
	private List<TryCatchBlockNode> tryCatchBlocks;
	private Set<SyntheticLocalVar> referencedSLV;
	private Map<String, Method> staticAnalyses;
	protected boolean usesDynamicAnalysis;

	public SnippetCode(InsnList instructions,
			List<TryCatchBlockNode> tryCatchBlocks,
			Set<SyntheticLocalVar> referencedSLV,
			Map<String, Method> staticAnalyses, boolean usesDynamicAnalysis) {
		super();
		this.instructions = instructions;
		this.tryCatchBlocks = tryCatchBlocks;
		this.referencedSLV = referencedSLV;
		this.staticAnalyses = staticAnalyses;
		this.usesDynamicAnalysis = usesDynamicAnalysis;
	}

	public InsnList getInstructions() {
		return instructions;
	}

	public List<TryCatchBlockNode> getTryCatchBlocks() {
		return tryCatchBlocks;
	}

	public Set<SyntheticLocalVar> getReferencedSLV() {
		return referencedSLV;
	}

	public Map<String, Method> getStaticAnalyses() {
		return staticAnalyses;
	}

	public boolean usesDynamicAnalysis() {
		return usesDynamicAnalysis;
	}

	public SnippetCode clone() {

		Map<LabelNode, LabelNode> map = new HashMap<LabelNode, LabelNode>();

		InsnList newInstructions = new InsnList();

		// First iterate the instruction list and get all the labels
		for (AbstractInsnNode instr : instructions.toArray()) {
			if (instr instanceof LabelNode) {
				LabelNode label = InsnListHelper.createLabel();
				map.put((LabelNode) instr, label);
			}
		}

		// then copy instructions using clone
		AbstractInsnNode instr = instructions.getFirst();

		while (instr != null) {

			// special case where we put a new label instead of old one
			if (instr instanceof LabelNode) {

				newInstructions.add(map.get(instr));

				instr = instr.getNext();

				continue;
			}

			newInstructions.add(instr.clone(map));

			instr = instr.getNext();
		}

		List<TryCatchBlockNode> newTryCatchBlocks = 
			new LinkedList<TryCatchBlockNode>();

		for (TryCatchBlockNode tcb : tryCatchBlocks) {

			newTryCatchBlocks.add(new TryCatchBlockNode(map.get(tcb.start),
					map.get(tcb.end), map.get(tcb.handler), tcb.type));
		}

		return new SnippetCode(newInstructions, newTryCatchBlocks,
				new HashSet<SyntheticLocalVar>(referencedSLV),
				new HashMap<String, Method>(staticAnalyses),
				usesDynamicAnalysis);
	}
}
