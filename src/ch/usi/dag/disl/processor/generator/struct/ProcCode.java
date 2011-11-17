package ch.usi.dag.disl.processor.generator.struct;

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

public class ProcCode extends Code {

	private boolean usesArgumentContext;

	public ProcCode(InsnList instructions,
			List<TryCatchBlockNode> tryCatchBlocks,
			Set<SyntheticLocalVar> referencedSLV,
			Set<ThreadLocalVar> referencedTLV,
			boolean containsHandledException,
			Set<StaticContextMethod> staticContexts,
			boolean usesDynamicContext,
			boolean usesArgumentContext
			) {
		
		super(instructions, tryCatchBlocks, referencedSLV, referencedTLV,
				staticContexts, usesDynamicContext, containsHandledException);
		this.usesArgumentContext = usesArgumentContext;
	}

	public boolean usesArgumentContext() {
		return usesArgumentContext;
	}
	
	public ProcCode clone() {

		Map<LabelNode, LabelNode> map = 
			AsmHelper.createLabelMap(getInstructions());

		return new ProcCode(AsmHelper.cloneInsnList(getInstructions(), map),
				AsmHelper.cloneTryCatchBlocks(getTryCatchBlocks(), map),
				new HashSet<SyntheticLocalVar>(getReferencedSLVs()),
				new HashSet<ThreadLocalVar>(getReferencedTLVs()),
				containsHandledException(),
				new HashSet<StaticContextMethod>(getStaticContexts()),
				usesDynamicContext(),
				usesArgumentContext);
	}
}
