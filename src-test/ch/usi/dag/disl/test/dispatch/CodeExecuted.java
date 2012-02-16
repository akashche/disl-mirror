package ch.usi.dag.disl.test.dispatch;

import ch.usi.dag.dislreserver.objectid.InvalidClass;
import ch.usi.dag.dislreserver.objectid.ObjectId;
import ch.usi.dag.dislreserver.remoteanalysis.RemoteAnalysis;

// NOTE that this class is not static anymore
public class CodeExecuted extends RemoteAnalysis {

	long totalExecutedBytecodes = 0;
	
	public void bytecodesExecuted(int count) {
		totalExecutedBytecodes += count;
	}
	
	public void testingBasic(boolean b, byte by, char c, short s, int i, long l,
			float f, double d) {

		if(b != true) {
			throw new RuntimeException("Incorect transfer of boolean");
		}
		
		if(by != (byte) 125) {
			throw new RuntimeException("Incorect transfer of byte");
		}
		
		if(c != 'š') {
			throw new RuntimeException("Incorect transfer of char");
		}
		
		if(s != (short) 50000) {
			throw new RuntimeException("Incorect transfer of short");
		}
		
		if(i != 100000) {
			throw new RuntimeException("Incorect transfer of int");
		}
		
		if(l != 10000000000L) {
			throw new RuntimeException("Incorect transfer of long");
		}
		
		if(f != 1.5F) {
			throw new RuntimeException("Incorect transfer of float");
		}
		
		if(d != 2.5) {
			throw new RuntimeException("Incorect transfer of double");
		}
	}
	
	public static void testingAdvanced(String s, Object o, Class<?> c,
			int classID) {

		if(! s.equals("ěščřžýáíé")) {
			throw new RuntimeException("Incorect transfer of String");
		}

		// note that id will be 0 if this is first transfer of Object
		if(! (o instanceof ObjectId) || ((ObjectId)o).getId() != 0) {
			throw new RuntimeException("Incorect transfer of Object");
		}
		
		// note that id will be 0 if this is first transfer of Class
		if(! c.equals(InvalidClass.class) || classID != 0) {
			throw new RuntimeException("Incorect transfer of Class");
		}
	}
	
	public void atExit() {
		System.out.println("Total number of executed bytecodes: "
				+ totalExecutedBytecodes);
	}
}
