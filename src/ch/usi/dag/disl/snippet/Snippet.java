package ch.usi.dag.disl.snippet;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;

import ch.usi.dag.disl.snippet.marker.Marker;
import ch.usi.dag.disl.snippet.scope.Scope;
import ch.usi.dag.disl.util.InsnListHelper;
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
		InsnListHelper.removeReturns(insnList);
		
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
		// TODO fix
		LabelNode tryBegin = new LabelNode();
		insnList.insert(tryBegin);
		
		// add invocation of activate at the beginning
		insnList.insert(mtdActivate);
		
		// add try label at the end
		// TODO fix
		LabelNode tryEnd = new LabelNode();
		insnList.add(tryEnd);
		
		// add invocation of deactivate at the end - normal flow
		insnList.add(mtdDeactivate);
		
		// TODO create try catch block
		
		// TODO add try catch block
		
		// TODO add try catch block code
	}
}
