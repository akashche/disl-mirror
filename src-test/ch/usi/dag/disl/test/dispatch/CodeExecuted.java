package ch.usi.dag.disl.test.dispatch;

import ch.usi.dag.dislreserver.classid.InvalidClass;
import ch.usi.dag.dislreserver.netreference.NetReference;
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
			int classId) {

		if(! s.equals("ěščřžýáíé")) {
			throw new RuntimeException("Incorect transfer of String");
		}

		long objId = ((NetReference)o).getObjectId();
		
		// object id should be non 0
		if(! (o instanceof NetReference) || objId == 0) {
			throw new RuntimeException("Incorect transfer of Object");
		}
		
		System.out.println("Received object id: " + objId);
		
		// class id should be non 0
		if(! c.equals(InvalidClass.class) || classId == 0) {
			throw new RuntimeException("Incorect transfer of Class");
		}
		
		System.out.println("Received class id: " + classId);
	}
	
	public void atExit() {
		System.out.println("Total number of executed bytecodes: "
				+ totalExecutedBytecodes);
	}

	public void objectFree(NetReference netRef) {
		System.out.println("Object free for id " + netRef.getObjectId());
		
	}
}
