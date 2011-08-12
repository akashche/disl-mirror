package ch.usi.dag.disl.snippet;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import ch.usi.dag.disl.snippet.localvars.SyntheticLocalVar;
import ch.usi.dag.disl.snippet.localvars.ThreadLocalVar;
import ch.usi.dag.disl.util.AsmHelper;

public class SnippetCode {

	private InsnList instructions;
	private List<TryCatchBlockNode> tryCatchBlocks;
	private Set<SyntheticLocalVar> referencedSLV;
	private Set<ThreadLocalVar> referencedTLV;
	private Map<String, Method> staticAnalyses;
	private boolean usesDynamicAnalysis;
	// the code contains handler that handles exception and doesn't propagate
	// it further - can cause stack inconsistency that has to be handled
	private boolean containsHandledException;

	public SnippetCode(InsnList instructions,
			List<TryCatchBlockNode> tryCatchBlocks,
			Set<SyntheticLocalVar> referencedSLV,
			Set<ThreadLocalVar> referencedTLV,
			Map<String, Method> staticAnalyses,
			boolean usesDynamicAnalysis,
			boolean containsHandledException
			) {
		super();
		this.instructions = instructions;
		this.tryCatchBlocks = tryCatchBlocks;
		this.referencedSLV = referencedSLV;
		this.referencedTLV = referencedTLV;
		this.staticAnalyses = staticAnalyses;
		this.usesDynamicAnalysis = usesDynamicAnalysis;
		this.containsHandledException = containsHandledException;
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
	
	public Set<ThreadLocalVar> getReferencedTLV() {
		return referencedTLV;
	}

	public Map<String, Method> getStaticAnalyses() {
		return staticAnalyses;
	}

	public boolean usesDynamicAnalysis() {
		return usesDynamicAnalysis;
	}
	
	public boolean containsHandledException() {
		return containsHandledException;
	}

	public SnippetCode clone() {

		Map<LabelNode, LabelNode> map = AsmHelper.createLabelMap(instructions);

		return new SnippetCode(AsmHelper.cloneInsnList(instructions, map),
				AsmHelper.cloneTryCatchBlocks(tryCatchBlocks, map),
				new HashSet<SyntheticLocalVar>(referencedSLV),
				new HashSet<ThreadLocalVar>(referencedTLV),
				new HashMap<String, Method>(staticAnalyses),
				usesDynamicAnalysis,
				containsHandledException);
	}
}
