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


	protected Map<String, Method> staticAnalyses;
	protected boolean usesDynamicAnalysis;


	public SnippetCode(InsnList instructions,
			List<TryCatchBlockNode> tryCatchBlocks,
			Set<SyntheticLocalVar> referencedSLV,
			Set<ThreadLocalVar> referencedTLV,
			boolean containsHandledException,
			Map<String, Method> staticAnalyses,
			boolean usesDynamicAnalysis
			) {
		
		super(instructions, tryCatchBlocks, referencedSLV, referencedTLV,
				containsHandledException);
		this.staticAnalyses = staticAnalyses;
		this.usesDynamicAnalysis = usesDynamicAnalysis;
	}

	public Map<String, Method> getStaticAnalyses() {
		return staticAnalyses;
	}

	public boolean usesDynamicAnalysis() {
		return usesDynamicAnalysis;
	}
	
	public SnippetCode clone() {

		Map<LabelNode, LabelNode> map = 
			AsmHelper.createLabelMap(getInstructions());

		return new SnippetCode(AsmHelper.cloneInsnList(getInstructions(), map),
				AsmHelper.cloneTryCatchBlocks(getTryCatchBlocks(), map),
				new HashSet<SyntheticLocalVar>(getReferencedSLV()),
				new HashSet<ThreadLocalVar>(getReferencedTLV()),
				containsHandledException(),
				new HashMap<String, Method>(staticAnalyses),
				usesDynamicAnalysis);
	}
}
