package ch.usi.dag.disl.snippet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import ch.usi.dag.disl.coderep.Code;
import ch.usi.dag.disl.localvar.SyntheticLocalVar;
import ch.usi.dag.disl.localvar.ThreadLocalVar;
import ch.usi.dag.disl.util.AsmHelper;

public class SnippetCode extends Code {

	private Map<String, StaticAnalysisMethod> staticAnalyses;
	private boolean usesDynamicAnalysis;
	// integer (key) is an index of an instruction in snippet code that invokes
	// processor
	private Map<Integer, ProcInvocation> invokedProcessors; 

	public SnippetCode(InsnList instructions,
			List<TryCatchBlockNode> tryCatchBlocks,
			Set<SyntheticLocalVar> referencedSLV,
			Set<ThreadLocalVar> referencedTLV,
			boolean containsHandledException,
			Map<String, StaticAnalysisMethod> staticAnalyses,
			boolean usesDynamicAnalysis,
			Map<Integer, ProcInvocation> invokedProcessors
			) {
		
		super(instructions, tryCatchBlocks, referencedSLV, referencedTLV,
				containsHandledException);
		this.staticAnalyses = staticAnalyses;
		this.usesDynamicAnalysis = usesDynamicAnalysis;
		this.invokedProcessors = invokedProcessors;
	}

	public Map<String, StaticAnalysisMethod> getStaticAnalyses() {
		return staticAnalyses;
	}

	public boolean usesDynamicAnalysis() {
		return usesDynamicAnalysis;
	}
	
	public Map<Integer, ProcInvocation> getInvokedProcessors() {
		return invokedProcessors;
	}
	
	public SnippetCode clone() {

		Map<LabelNode, LabelNode> map = 
			AsmHelper.createLabelMap(getInstructions());

		return new SnippetCode(AsmHelper.cloneInsnList(getInstructions(), map),
				AsmHelper.cloneTryCatchBlocks(getTryCatchBlocks(), map),
				new HashSet<SyntheticLocalVar>(getReferencedSLVs()),
				new HashSet<ThreadLocalVar>(getReferencedTLVs()),
				containsHandledException(),
				new HashMap<String, StaticAnalysisMethod>(staticAnalyses),
				usesDynamicAnalysis,
				new HashMap<Integer, ProcInvocation>(invokedProcessors));
	}
}
