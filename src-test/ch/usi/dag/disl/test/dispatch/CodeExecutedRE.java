package ch.usi.dag.disl.test.dispatch;

import ch.usi.dag.dislre.REDispatch;

// Optimally, this class is automatically created on analysis machine
// and redefines during loading the CodeExecuted class on the client vm

// Even more optimally, this is automatically generated native class with same
// functionality
public class CodeExecutedRE {

	public static void bytecodesExecuted(int count) {
		
		int sid = REDispatch.analysisStart(1);
		
		REDispatch.sendInt(sid, count);
		
		REDispatch.analysisEnd(sid);
	}
	
	public static void testingBasic(boolean b, byte by, char c, short s, int i,
			long l, float f, double d) {
		
		int sid = REDispatch.analysisStart(2);
		
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
	
	public static void testingAdvanced(String s, Object o, Class<?> c, int classID) {
		
		int sid = REDispatch.analysisStart(3);
		
		REDispatch.sendString(sid, s);
		REDispatch.sendObject(sid, o);
		REDispatch.sendClass(sid, c);
		// class_id ignored
		
		REDispatch.analysisEnd(sid);
	}
}
