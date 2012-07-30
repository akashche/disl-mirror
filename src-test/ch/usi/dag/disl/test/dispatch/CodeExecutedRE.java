package ch.usi.dag.disl.test.dispatch;

import ch.usi.dag.dislre.REDispatch;

// Optimally, this class is automatically created on analysis machine
// and redefines during loading the CodeExecuted class on the client vm

// Even more optimally, this is automatically generated native class with same
// functionality
public class CodeExecutedRE {

	private static short beId = REDispatch.registerMethod(
			"ch.usi.dag.disl.test.dispatch.CodeExecuted.bytecodesExecuted");

	private static short tbId = REDispatch.registerMethod(
			"ch.usi.dag.disl.test.dispatch.CodeExecuted.testingBasic");
	
	private static short taId = REDispatch.registerMethod(
			"ch.usi.dag.disl.test.dispatch.CodeExecuted.testingAdvanced");
	
	private static short ta2Id = REDispatch.registerMethod(
			"ch.usi.dag.disl.test.dispatch.CodeExecuted.testingAdvanced2");
	
	public static void bytecodesExecuted(int count) {
		
		REDispatch.analysisStart(beId);
		
		REDispatch.sendInt(count);
		
		REDispatch.analysisEnd();
	}
	
	public static void testingBasic(boolean b, byte by, char c, short s, int i,
			long l, float f, double d) {
		
		REDispatch.analysisStart(tbId);
		
		REDispatch.sendBoolean(b);
		REDispatch.sendByte(by);
		REDispatch.sendChar(c);
		REDispatch.sendShort(s);
		REDispatch.sendInt(i);
		REDispatch.sendLong(l);
		REDispatch.sendFloat(f);
		REDispatch.sendDouble(d);
		
		REDispatch.analysisEnd();
	}
	
	public static void testingAdvanced(String s, Object o, Class<?> c,
			int classID) {
		
		REDispatch.analysisStart(taId);
		
		REDispatch.sendString(s);
		REDispatch.sendObject(o);
		REDispatch.sendClass(c);
		// class_id ignored
		
		REDispatch.analysisEnd();
	}

	public static void testingAdvanced2(Object o1, Object o2, Object o3,
			Object o4, Class<?> class1, int cid1, Class<?> class2, int cid2,
			Class<?> class3, int cid3, Class<?> class4, int cid4) {

		REDispatch.analysisStart(ta2Id);
		
		REDispatch.sendObject(o1);
		REDispatch.sendObject(o2);
		REDispatch.sendObject(o3);
		REDispatch.sendObject(o4);
		REDispatch.sendClass(class1);
		// class_id ignored
		REDispatch.sendClass(class2);
		// class_id ignored
		REDispatch.sendClass(class3);
		// class_id ignored
		REDispatch.sendClass(class4);
		// class_id ignored
		
		REDispatch.analysisEnd();		
	}
}
