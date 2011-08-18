package ch.usi.dag.disl.dislclass.code;

import java.util.List;
import java.util.Set;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.TryCatchBlockNode;

import ch.usi.dag.disl.dislclass.localvar.SyntheticLocalVar;
import ch.usi.dag.disl.dislclass.localvar.ThreadLocalVar;

public class Code {

	protected InsnList instructions;
	protected List<TryCatchBlockNode> tryCatchBlocks;
	protected Set<SyntheticLocalVar> referencedSLV;
	protected Set<ThreadLocalVar> referencedTLV;
	// the code contains handler that handles exception and doesn't propagate
	// it further - can cause stack inconsistency that has to be handled
	protected boolean containsHandledException;
	
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

	public boolean containsHandledException() {
		return containsHandledException;
	}
	
}
