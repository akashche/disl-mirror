package ch.usi.dag.disl.test.dispatch;

import ch.usi.dag.dislre.REDispatch;

// Optimally, this class is automatically created on analysis machine
// and redefines during loading the CodeExecuted class on the client vm

// Even more optimally, this is automatically generated native class with same
// functionality
public class CodeExecutedRE {

	public static void bytecodesExecuted(int count) {
		
		int sid = REDispatch.analysisStart(
				"ch.usi.dag.disl.test.dispatch.CodeExecuted.bytecodesExecuted");
		
		REDispatch.sendInt(sid, count);
		
		REDispatch.analysisEnd(sid);
	}
	
	public static void testingBasic(boolean b, byte by, char c, short s, int i,
			long l, float f, double d) {
		
		int sid = REDispatch.analysisStart(
				"ch.usi.dag.disl.test.dispatch.CodeExecuted.testingBasic");
		
		REDispatch.sendBoolean(sid, b);
		REDispatch.sendByte(sid, by);
		REDispatch.sendChar(sid, c);
		REDispatch.sendShort(sid, s);
		REDispatch.sendInt(sid, i);
		REDispatch.sendLong(sid, l);
		REDispatch.sendFloat(sid, f);
		REDispatch.sendDouble(sid, d);
		
		REDispatch.analysisEnd(sid);
	}
	
	public static void testingAdvanced(String s, Object o, Class<?> c,
			int classID) {
		
		int sid = REDispatch.analysisStart(
				"ch.usi.dag.disl.test.dispatch.CodeExecuted.testingAdvanced");
		
		REDispatch.sendString(sid, s);
		REDispatch.sendObject(sid, o);
		REDispatch.sendClass(sid, c);
		// class_id ignored
		
		REDispatch.analysisEnd(sid);
	}

	public static void testingAdvanced2(Object o1, Object o2, Object o3,
			Object o4, Class<?> class1, int cid1, Class<?> class2, int cid2,
			Class<?> class3, int cid3, Class<?> class4, int cid4) {

		int sid = REDispatch.analysisStart(
				"ch.usi.dag.disl.test.dispatch.CodeExecuted.testingAdvanced2");
		
		REDispatch.sendObject(sid, o1);
		REDispatch.sendObject(sid, o2);
		REDispatch.sendObject(sid, o3);
		REDispatch.sendObject(sid, o4);
		REDispatch.sendClass(sid, class1);
		// class_id ignored
		REDispatch.sendClass(sid, class2);
		// class_id ignored
		REDispatch.sendClass(sid, class3);
		// class_id ignored
		REDispatch.sendClass(sid, class4);
		// class_id ignored
		
		REDispatch.analysisEnd(sid);		
	}
}
