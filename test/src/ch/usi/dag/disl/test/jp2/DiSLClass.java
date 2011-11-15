package ch.usi.dag.disl.test.jp2;

import ch.usi.dag.disl.annotation.AfterReturning;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.annotation.SyntheticLocal;
import ch.usi.dag.disl.annotation.SyntheticLocal.Initialize;
import ch.usi.dag.disl.annotation.ThreadLocal;
import ch.usi.dag.disl.marker.BasicBlockMarker;
import ch.usi.dag.disl.marker.BodyMarker;
import ch.usi.dag.disl.marker.BytecodeMarker;
import ch.usi.dag.disl.staticcontext.BasicBlockAnalysis;
import ch.usi.dag.disl.staticcontext.BytecodeAnalysis;
import ch.usi.dag.disl.staticcontext.StaticContext;
import ch.usi.dag.disl.test.jp2.runtime.CCTNode;


public class DiSLClass {

	@SyntheticLocal(initialize=Initialize.NEVER)
	private static CCTNode caller;

	@SyntheticLocal(initialize=Initialize.NEVER)
	private static CCTNode callee;

	@SyntheticLocal(initialize=Initialize.ALWAYS)
	private static int index = 1;

	@ThreadLocal
	static CCTNode threadLocalCCTNode;

	@ThreadLocal
	static int bytecodeIndex;

	
	@Before(marker=BodyMarker.class, scope= "*.*(...)", order=1)
	public static void methodEnter(StaticContext sc, JP2Analysis ba) {

		//	System.out.println(" METHOD " + sc.thisMethodFullName() + " has " + ba.getNumberOfBBs());


		caller =  threadLocalCCTNode;
		index =  bytecodeIndex;
		if(caller == null) {
			caller = CCTNode.getRoot();
			threadLocalCCTNode = caller;
		}
		String methodID =  sc.thisMethodFullName(); 
	//	System.out.println("WILL PROFILE " + methodID + " index " + index + " #of BBs " + ba.getNumberOfBBs());

		CCTNode.bbSizes.put(methodID,  new int[ba.getNumberOfBBs()]);
		callee = caller.profileCall(methodID, index, ba.getNumberOfBBs());

	}

	@AfterReturning(marker=BodyMarker.class, scope= "*.*(...)", order=1)
	public static void methodExit() {

	//	System.out.println("RETORING INDEX " + index + " ON CURRENT NODE " + caller);
		bytecodeIndex = index;
	    threadLocalCCTNode = caller;

	}

	@Before(marker=BasicBlockMarker.class, scope= "*.*(...)", order=2)
	public static void basicBlockEnter(BasicBlockAnalysis bba) {

		callee.setBBSize(bba.getBBindex(),bba.getBBSize());
		callee.incrementBytecodeCounter(bba.getBBSize());
		callee.profileBB(bba.getBBindex());

	}


	@Before(marker=BytecodeMarker.class,
			param= "new,invokevirtual,invokestatic,invokeinterface," +
			"invokedynamic,anewarray,checkcast,getstatic,instanceof," +
			"multianewarray,putstatic,ldc",

			scope= "*.*(...)" , order = 3 )
			public static void invocationTracer(BytecodeAnalysis bca, JP2Analysis ba ) {

	//	System.out.println("  INDEX OF BYTECODE " + ba.getInMethodIndex() + " with Opcode " + bca.getBytecodeNumber()) ;

		bytecodeIndex  = ba.getInMethodIndex();

	}


	@Before(marker=BytecodeMarker.class,
			param= "invokespecial",
			scope= "*.*(...)" , order = 4 )
			public static void invocationToObjectConstructor(JP2Analysis ba ) {
		
			if(ba.isCallToObjectConstructor()) {
				String objinit = "java/lang/Object.<init>";
				callee.profileCall(objinit, bytecodeIndex, 1);
				CCTNode.bbSizes.put(objinit, new int[]{1});
			}
			bytecodeIndex  = ba.getInMethodIndex();
	}
}
