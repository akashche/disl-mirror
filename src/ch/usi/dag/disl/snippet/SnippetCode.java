package ch.usi.dag.disl.snippet;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import ch.usi.dag.disl.snippet.localvars.SyntheticLocalVar;
import ch.usi.dag.disl.snippet.localvars.ThreadLocalVar;
import ch.usi.dag.disl.util.AsmHelper;
import ch.usi.dag.jborat.runtime.DynamicBypass;

public class SnippetCode {

	private InsnList instructions;
	private List<TryCatchBlockNode> tryCatchBlocks;
	private Set<SyntheticLocalVar> referencedSLV;
	private Set<ThreadLocalVar> referencedTLV;
	private Map<String, Method> staticAnalyses;
	protected boolean usesDynamicAnalysis;

	public SnippetCode(InsnList instructions,
			List<TryCatchBlockNode> tryCatchBlocks,
			Set<SyntheticLocalVar> referencedSLV,
			Set<ThreadLocalVar> referencedTLV,
			Map<String, Method> staticAnalyses, boolean usesDynamicAnalysis) {
		super();
		this.instructions = instructions;
		this.tryCatchBlocks = tryCatchBlocks;
		this.referencedSLV = referencedSLV;
		this.referencedTLV = referencedTLV;
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
	
	public Set<ThreadLocalVar> getReferencedTLV() {
		return referencedTLV;
	}

	public Map<String, Method> getStaticAnalyses() {
		return staticAnalyses;
	}

	public boolean usesDynamicAnalysis() {
		return usesDynamicAnalysis;
	}

	public SnippetCode clone() {

		Map<LabelNode, LabelNode> map = AsmHelper.createLabelMap(instructions);

		return new SnippetCode(AsmHelper.cloneInsnList(instructions, map),
				AsmHelper.cloneTryCatchBlocks(tryCatchBlocks, map),
				new HashSet<SyntheticLocalVar>(referencedSLV),
				new HashSet<ThreadLocalVar>(referencedTLV),
				new HashMap<String, Method>(staticAnalyses),
				usesDynamicAnalysis);
	}
	
	public void prepare(boolean useDynamicBypass) {
		
		// remove returns in snippet (in asm code)
		AsmHelper.removeReturns(instructions);
		
		if(! useDynamicBypass) {
			return;
		}

		// *** dynamic bypass ***
		
		// inserts
		// DynamicBypass.activate();
		// try {
		//     ... original code
		// } finally {
		//     DynamicBypass.deactivate();
		// }
		
		// create method nodes
		Type typeDB = Type.getType(DynamicBypass.class);
		MethodInsnNode mtdActivate = new MethodInsnNode(Opcodes.INVOKESTATIC,
				typeDB.getInternalName(), "activate", "()V");
		MethodInsnNode mtdDeactivate = new MethodInsnNode(Opcodes.INVOKESTATIC,
				typeDB.getInternalName(), "deactivate", "()V");
		
		// add try label at the beginning
		LabelNode tryBegin = new LabelNode();
		instructions.insert(tryBegin);
		
		// add invocation of activate at the beginning
		instructions.insert(mtdActivate.clone(null));

		// ## try {
		
		// ## }
		
		// add try label at the end
		LabelNode tryEnd = new LabelNode();
		instructions.add(tryEnd);
		
		// ## after normal flow
		
		// add invocation of deactivate - normal flow
		instructions.add(mtdDeactivate.clone(null));
		
		// normal flow should jump after handler
		LabelNode handlerEnd = new LabelNode();
		instructions.add(new JumpInsnNode(Opcodes.GOTO, handlerEnd));

		// ## after abnormal flow - exception handler
		
		// add handler begin
		LabelNode handlerBegin = new LabelNode();
		instructions.add(handlerBegin);
		
		// add invocation of deactivate - abnormal flow
		instructions.add(mtdDeactivate.clone(null));
		// throw exception again
		instructions.add(new InsnNode(Opcodes.ATHROW));
		
		// add handler end
		instructions.add(handlerEnd);
		
		// ## add handler to the list
		tryCatchBlocks.add(
				new TryCatchBlockNode(tryBegin, tryEnd, handlerBegin, null));
	}
}
