package ch.usi.dag.disl.coderep;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.TryCatchBlockNode;

import ch.usi.dag.disl.localvar.SyntheticLocalVar;
import ch.usi.dag.disl.localvar.ThreadLocalVar;
import ch.usi.dag.disl.util.AsmHelper;
import ch.usi.dag.disl.util.AsmHelper.ClonedCode;

public class Code {

	private InsnList instructions;
	private List<TryCatchBlockNode> tryCatchBlocks;
	private Set<SyntheticLocalVar> referencedSLVs;
	private Set<ThreadLocalVar> referencedTLVs;
	private Set<StaticContextMethod> staticContexts;
	private boolean usesDynamicContext;
	private boolean usesClassContext;
	// the code contains handler that handles exception and doesn't propagate
	// it further - can cause stack inconsistency that has to be handled
	private boolean containsHandledException;

	public Code(InsnList instructions, List<TryCatchBlockNode> tryCatchBlocks,
			Set<SyntheticLocalVar> referencedSLVs,
			Set<ThreadLocalVar> referencedTLVs,
			Set<StaticContextMethod> staticContexts,
			boolean usesDynamicContext,
			boolean usesClassContext,
			boolean containsHandledException) {
		super();
		this.instructions = instructions;
		this.tryCatchBlocks = tryCatchBlocks;
		this.referencedSLVs = referencedSLVs;
		this.referencedTLVs = referencedTLVs;
		this.staticContexts = staticContexts;
		this.usesDynamicContext = usesDynamicContext;
		this.usesClassContext = usesClassContext; 
		this.containsHandledException = containsHandledException;
	}
	
	public InsnList getInstructions() {
		return instructions;
	}

	public List<TryCatchBlockNode> getTryCatchBlocks() {
		return tryCatchBlocks;
	}

	public Set<SyntheticLocalVar> getReferencedSLVs() {
		return referencedSLVs;
	}
	
	public Set<ThreadLocalVar> getReferencedTLVs() {
		return referencedTLVs;
	}

	public Set<StaticContextMethod> getStaticContexts() {
		return staticContexts;
	}

	public boolean usesDynamicContext() {
		return usesDynamicContext;
	}
	
	public boolean usesClassContext() {
		return usesClassContext;
	}
	
	public boolean containsHandledException() {
		return containsHandledException;
	}
	
	public Code clone() {
		
		// clone code first
		ClonedCode cc = 
				AsmHelper.cloneCode(instructions, tryCatchBlocks);
		
		return new Code(cc.getInstructions(), cc.getTryCatchBlocks(),
				new HashSet<SyntheticLocalVar>(referencedSLVs),
				new HashSet<ThreadLocalVar>(referencedTLVs),
				new HashSet<StaticContextMethod>(staticContexts),
				usesDynamicContext, usesClassContext, containsHandledException);
	}
}
