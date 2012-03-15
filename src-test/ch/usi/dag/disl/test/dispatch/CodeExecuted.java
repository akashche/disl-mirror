package ch.usi.dag.disl.test.dispatch;

import ch.usi.dag.dislreserver.netreference.NetReference;
import ch.usi.dag.dislreserver.reflectiveinfo.ClassInfo;
import ch.usi.dag.dislreserver.reflectiveinfo.ClassInfoResolver;
import ch.usi.dag.dislreserver.reflectiveinfo.InvalidClass;
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
	
	public static void printClassInfo(ClassInfo ci) {
		
		if(ci == null) {
			System.out.println("null");
			return;
		}
		
		System.out.println("sig: " + ci.getSignature());
		System.out.println("gen: " + ci.getGenericStr());
	}
	
	public static void testingAdvanced2(Object o1, Object o2, Object o3,
			Object o4, Class<?> class1, int cid1, Class<?> class2, int cid2,
			Class<?> class3, int cid3, Class<?> class4, int cid4) {
		
		System.out.println("* o1 class *");
		printClassInfo(((NetReference)o1).getClassInfo());
		
		System.out.println("* o2 class *");
		printClassInfo(((NetReference)o2).getClassInfo());
		
		System.out.println("* o3 class *");
		printClassInfo(((NetReference)o3).getClassInfo());
		
		System.out.println("* o4 class *");
		printClassInfo(((NetReference)o4).getClassInfo());
		
		System.out.println("* class 1 *");
		printClassInfo(ClassInfoResolver.getClass(cid1));
		
		System.out.println("* class 2 *");
		printClassInfo(ClassInfoResolver.getClass(cid2));
		
		System.out.println("* class 3 *");
		printClassInfo(ClassInfoResolver.getClass(cid3));
		
		System.out.println("* class 4 *");
		printClassInfo(ClassInfoResolver.getClass(cid4));
	}
	
	public void atExit() {
		System.out.println("Total number of executed bytecodes: "
				+ totalExecutedBytecodes);
	}

	public void objectFree(NetReference netRef) {
		System.out.println("Object free for id " + netRef.getObjectId());
		
	}
}
