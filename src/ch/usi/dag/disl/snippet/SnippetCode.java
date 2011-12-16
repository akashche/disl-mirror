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
import ch.usi.dag.disl.coderep.StaticContextMethod;
import ch.usi.dag.disl.localvar.SyntheticLocalVar;
import ch.usi.dag.disl.localvar.ThreadLocalVar;
import ch.usi.dag.disl.util.AsmHelper;

public class SnippetCode extends Code {

	private boolean usesProcessorContext;
	// integer (key) is an index of an instruction in snippet code that invokes
	// processor
	private Map<Integer, ProcInvocation> invokedProcessors;

	public SnippetCode(InsnList instructions,
			List<TryCatchBlockNode> tryCatchBlocks,
			Set<SyntheticLocalVar> referencedSLV,
			Set<ThreadLocalVar> referencedTLV,
			boolean containsHandledException,
			Set<StaticContextMethod> staticContexts,
			boolean usesDynamicContext,
			boolean usesClassContext,
			boolean usesProcessorContext,
			Map<Integer, ProcInvocation> invokedProcessors
			) {
		
		super(instructions, tryCatchBlocks, referencedSLV, referencedTLV,
				staticContexts, usesDynamicContext, usesClassContext,
				containsHandledException);
		this.invokedProcessors = invokedProcessors;
		this.usesProcessorContext = usesProcessorContext;
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
				new HashSet<StaticContextMethod>(getStaticContexts()),
				usesDynamicContext(),
				usesClassContext(),
				usesProcessorContext,
				new HashMap<Integer, ProcInvocation>(invokedProcessors));
	}
}
