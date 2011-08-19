package ch.usi.dag.disl.dislclass.snippet;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import ch.usi.dag.disl.dislclass.code.Code;
import ch.usi.dag.disl.dislclass.localvar.SyntheticLocalVar;
import ch.usi.dag.disl.dislclass.localvar.ThreadLocalVar;
import ch.usi.dag.disl.util.AsmHelper;

public class SnippetCode extends Code {

	private Map<String, Method> staticAnalyses;
	private boolean usesDynamicAnalysis;
	private Map<Integer, ProcInvocation> invokedProcessors; 

	public SnippetCode(InsnList instructions,
			List<TryCatchBlockNode> tryCatchBlocks,
			Set<SyntheticLocalVar> referencedSLV,
			Set<ThreadLocalVar> referencedTLV,
			boolean containsHandledException,
			Map<String, Method> staticAnalyses,
			boolean usesDynamicAnalysis,
			Map<Integer, ProcInvocation> invokedProcessors
			) {
		
		super(instructions, tryCatchBlocks, referencedSLV, referencedTLV,
				containsHandledException);
		this.staticAnalyses = staticAnalyses;
		this.usesDynamicAnalysis = usesDynamicAnalysis;
		this.invokedProcessors = invokedProcessors;
	}

	public Map<String, Method> getStaticAnalyses() {
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
				new HashMap<String, Method>(staticAnalyses),
				usesDynamicAnalysis,
				new HashMap<Integer, ProcInvocation>(invokedProcessors));
	}
}
