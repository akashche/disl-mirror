package ch.usi.dag.disl.test.dispatch;

import ch.usi.dag.dislre.REDispatch;

// Optimally, this class is automatically created on analysis machine
// and redefines during loading the CodeExecuted class on the client vm

// Even more optimally, this is automatically generated native class with same
// functionality
public class CodeExecutedRE {

	public static void bytecodesExecuted(int count) {
		
		REDispatch.analysisStart(1);
		
		REDispatch.sendInt(count);
		
		REDispatch.analysisEnd();
	}
	
	public static void testingBasic(boolean b, byte by, char c, short s, int i,
			long l, float f, double d) {
		
		REDispatch.analysisStart(2);
		
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
	
	public static void testingAdvanced(String s, Object o, Class<?> c, int classID) {
		
		REDispatch.analysisStart(3);
		
		REDispatch.sendString(s);
		REDispatch.sendObject(o);
		REDispatch.sendClass(c);
		// class_id ignored
		
		REDispatch.analysisEnd();
	}
}
