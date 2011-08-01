package ch.usi.dag.disl.snippet;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import ch.usi.dag.disl.snippet.marker.Marker;
import ch.usi.dag.disl.snippet.scope.Scope;
import ch.usi.dag.disl.util.AsmHelper;
import ch.usi.dag.jborat.runtime.DynamicBypass;

public class Snippet implements Comparable<Snippet> {

	protected Class<?> annotationClass;
	protected Marker marker;
	protected Scope scope;
	protected int order;
	protected SnippetCode code;
	
	public Snippet(Class<?> annotationClass, Marker marker, Scope scope,
			int order, SnippetCode code) {
		super();

		this.annotationClass = annotationClass;
		this.marker = marker;
		this.scope = scope;
		this.order = order;
		this.code = code;
	}

	public Class<?> getAnnotationClass() {
		return annotationClass;
	}

	public Marker getMarker() {
		return marker;
	}

	public Scope getScope() {
		return scope;
	}

	public int getOrder() {
		return order;
	}

	public SnippetCode getCode() {
		return code;
	}
	
	public int compareTo(Snippet o) {
		return order - o.getOrder();
	}
	
	public void prepare(boolean useDynamicBypass) {
		
		InsnList insnList = code.getInstructions();
		
		// remove returns in snippet (in asm code)
		AsmHelper.removeReturns(insnList);
		
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
		MethodInsnNode mtdActivate = new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
				typeDB.getInternalName(), "activate", "()V");
		MethodInsnNode mtdDeactivate = new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
				typeDB.getInternalName(), "deactivate", "()V");
		
		// add try label at the beginning
		LabelNode tryBegin = new LabelNode();
		insnList.insert(tryBegin);
		
		// add invocation of activate at the beginning
		insnList.insert(mtdActivate);

		// ## try {
		
		// ## }
		
		// add try label at the end
		LabelNode tryEnd = new LabelNode();
		insnList.add(tryEnd);
		
		// ## after normal flow
		
		// add invocation of deactivate - normal flow
		insnList.add(mtdDeactivate);
		
		// normal flow should jump after handler
		LabelNode handlerEnd = new LabelNode();
		insnList.add(new JumpInsnNode(Opcodes.GOTO, handlerEnd));

		// ## after abnormal flow - exception handler
		
		// add handler begin
		LabelNode handlerBegin = new LabelNode();
		insnList.add(handlerBegin);
		
		// high value - so we don't interfere with some other local  
		final int LOCAL_VAR_DYNAMIC_BYPASS = 1010101;

		// store exception
		insnList.add(new IntInsnNode(Opcodes.ASTORE, LOCAL_VAR_DYNAMIC_BYPASS));
		// add invocation of deactivate - abnormal flow
		insnList.add(mtdDeactivate);
		// load exception
		insnList.add(new IntInsnNode(Opcodes.ALOAD, LOCAL_VAR_DYNAMIC_BYPASS));
		// throw exception
		insnList.add(new InsnNode(Opcodes.ATHROW));
		
		// add handler end
		insnList.add(handlerEnd);
		
		// ## add handler to the list
		code.getTryCatchBlocks().add(
				new TryCatchBlockNode(tryBegin, tryEnd, handlerBegin, null));
	}
}
