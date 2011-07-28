package ch.usi.dag.disl.snippet;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.TryCatchBlockNode;

import ch.usi.dag.disl.snippet.syntheticlocal.SyntheticLocalVar;

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
	
	// TODO implement clone
}
