package ch.usi.dag.disl.example.jp2;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.annotation.SyntheticLocal;
import ch.usi.dag.disl.annotation.ThreadLocal;
import ch.usi.dag.disl.example.jp2.runtime.CCTNode;
import ch.usi.dag.disl.marker.BasicBlockMarker;
import ch.usi.dag.disl.marker.BodyMarker;
import ch.usi.dag.disl.marker.BytecodeMarker;
import ch.usi.dag.disl.staticcontext.BasicBlockStaticContext;
import ch.usi.dag.disl.staticcontext.BytecodeStaticContext;
import ch.usi.dag.disl.staticcontext.MethodStaticContext;



public class DiSLClass {

	@SyntheticLocal//(initialize=Initialize.NEVER)
	private static CCTNode caller;

	@SyntheticLocal//(initialize=Initialize.NEVER)
	private static CCTNode callee;

	@SyntheticLocal//(initialize=Initialize.ALWAYS)
	private static int index = 1;

	@ThreadLocal
	static CCTNode threadLocalCCTNode;

	@ThreadLocal
	static int bytecodeIndex;


	@Before(marker=BodyMarker.class, order = 0)
	public static void methodEnter(MethodStaticContext sc, JP2Analysis ba) {

		caller =  threadLocalCCTNode;
		index =  bytecodeIndex;
		if(caller == null) {
			caller = CCTNode.getRoot();
			threadLocalCCTNode = caller;
			index = -1;
		}
		String methodID =  sc.thisMethodFullName(); 
		CCTNode.bbSizes.put(methodID,  new int[ba.getNumberOfBBs()]);
		callee = caller.profileCall(methodID, index, ba.getNumberOfBBs());
		threadLocalCCTNode = callee;
	}

	@After(marker=BodyMarker.class)
	public static void methodExit(MethodStaticContext sc) {

		bytecodeIndex = index;
		threadLocalCCTNode = caller;
	}

	@Before(marker=BasicBlockMarker.class, order = 1)
	public static void basicBlockEnter(BasicBlockStaticContext bba) {
		callee.setBBSize(bba.getBBindex(),bba.getBBSize());
		callee.incrementBytecodeCounter(bba.getBBSize());
		callee.profileBB(bba.getBBindex());
	}


	@Before(marker=BytecodeMarker.class,
			args= "new,invokevirtual,invokestatic,invokeinterface," +
					"invokedynamic,anewarray,checkcast,getstatic,instanceof," +
			"multianewarray,putstatic,ldc")
	public static void invocationTracer(BytecodeStaticContext bca, JP2Analysis ba ) {

		bytecodeIndex  = ba.getInMethodIndex();

	}


	@Before(marker=BytecodeMarker.class,
			args= "invokespecial", guard=GuardObjectInit.class)
	public static void invocationToObjectConstructor(JP2Analysis ba ) {
		String objinit = "java/lang/Object.<init>";
		callee.profileCall(objinit, bytecodeIndex, 1);
		CCTNode.bbSizes.put(objinit, new int[]{1});
		bytecodeIndex  = ba.getInMethodIndex();
	}
}
