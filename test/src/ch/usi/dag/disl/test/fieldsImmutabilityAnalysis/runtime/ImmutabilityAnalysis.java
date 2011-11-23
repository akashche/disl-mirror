package ch.usi.dag.disl.test.fieldsImmutabilityAnalysis.runtime;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

import ch.usi.dag.jborat.runtime.DynamicBypass;



@SuppressWarnings("unchecked")
public class ImmutabilityAnalysis {
	private static final int NUMBER_OF_TABLES = 40000;
	private static MyWeakKeyIdentityHashMap<Object, ConcurrentHashMap<String, FieldState>> bigMap[];

	private static final String DUMP_FILE = System.getProperty("dump", "dump.log");

	private static MyDumper myDumper;
	
//	public static ConcurrentHashMap<String,String>objectTable = new ConcurrentHashMap<String, String>();
	
	static {
		boolean oldState = DynamicBypass.getAndSet();
		try{
			PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(DUMP_FILE)));
			myDumper = new MyDumper(ps);
			bigMap = new MyWeakKeyIdentityHashMap[NUMBER_OF_TABLES];

			for(int i=0;i<NUMBER_OF_TABLES;i++){
				bigMap[i]= new MyWeakKeyIdentityHashMap<Object, ConcurrentHashMap<String, FieldState>>(8, myDumper);
			}

			Thread shutdownHook = new Thread(){
				public void run() {
					DynamicBypass.set(true);
					System.err.println("In shutdown hook!");
					dump();
					myDumper.close();
				}
			};
			Runtime.getRuntime().addShutdownHook(shutdownHook);
		}
		catch(Throwable t){
			t.printStackTrace();
			System.exit(-1);
		}
		finally{
			DynamicBypass.set(oldState);
		}
	}

	public static void onFieldRead(Object accessedObj, String accessedFieldName, String accessSite ) {
		try{
			
			String objectID = getObjectID(accessedObj, null);
			FieldState fs = getOrCreateInstanceField(accessedObj, objectID, accessedFieldName);

			//		if(accessedObj instanceof java.io.ByteArrayInputStream) {
			//			System.out.println("****** " + accessedObj.getClass().getName() + "\t" + System.identityHashCode(accessedObj) + "\t" + os.toString());
			//			new Exception().printStackTrace();
			//		}

			fs.onRead();

			//		if(accessedObj instanceof java.io.ByteArrayInputStream) {
			//			System.out.println("************** " + os.toString()  + "\t" + Thread.currentThread().getName() + "\t" + Thread.currentThread().getId());
			//		}
		}
		catch(Throwable t){
			t.printStackTrace();
		}
	}

	public static void onObjectInitialization(Object allocatedObj, String allocSite) {
		int idx = System.identityHashCode(allocatedObj) % NUMBER_OF_TABLES;

		try{
			String objectID = getObjectID(allocatedObj, allocSite);
			synchronized (bigMap[idx]) {
				if(bigMap[idx].get(objectID) == null) {
					bigMap[idx].put(allocatedObj, new ConcurrentHashMap<String, FieldState>(), objectID);
				}	
			}
//			String allocatedSite;
//			synchronized (objectTable) {
//				if((allocatedSite = (String)objectTable.get(objectID)) == null)
//					objectTable.put(objectID,allocSite);
//			}
		}
		catch(Throwable t){
			t.printStackTrace();
		}
	}


	public static void onFieldWrite(Object accessedObj, String accessedFieldName, String accessSite, Stack<Object> stack) {
		try{
			boolean  isInDynamicExtendOfConstructor = false;
			if(stack != null) {
				if(isIncluded(stack, accessedObj)) {
					isInDynamicExtendOfConstructor = true;
				}
			}
			String objectID = getObjectID(accessedObj, null);
			FieldState fs = getOrCreateInstanceField(accessedObj, objectID, accessedFieldName);

			//		if(accessedObj instanceof java.io.ByteArrayInputStream) {
			//			System.out.println("~~~~~~ " + accessedObj.getClass().getName() + "\t" + System.identityHashCode(accessedObj) + "\t" + os.toString());
			//			new Exception().printStackTrace();
			//		}

			fs.onWrite(isInDynamicExtendOfConstructor);

			//		if(accessedObj instanceof java.io.ByteArrayInputStream) {
			//			System.out.println("~~~~~~~~~~~~~~ " + os.toString()  + "\t" + Thread.currentThread().getName() + "\t" + Thread.currentThread().getId());
			//		}
		}
		catch(Throwable t){
			t.printStackTrace();
		}
	}

	public static void afterConstructor(Stack<Object> stack) {
		if(stack != null) {
			stack.pop();
		} else {
			System.err.println("The stack is null " + Thread.currentThread().getName());
		}
	}

	private static FieldState getOrCreateInstanceField(Object accessedObj, String objectId, String fieldName) {
		int idx = System.identityHashCode(accessedObj) % NUMBER_OF_TABLES;
		try{
			ConcurrentHashMap<String, FieldState> smallMap;
			FieldState fs;
			
			synchronized(bigMap[idx]){
				if((smallMap = bigMap[idx].get(accessedObj)) == null) {
					bigMap[idx].put(accessedObj, smallMap = new ConcurrentHashMap<String, FieldState>(),objectId);
//					if(accessedObj instanceof String)
//						new Exception().printStackTrace();
				}
			}

			if((fs = smallMap.get(fieldName)) == null) {
				FieldState oldFS;
				if((oldFS = smallMap.putIfAbsent(fieldName, fs = new FieldState())) != null) {
					fs = oldFS;
				}
			}
			return fs;
		}
		catch(Throwable t){
			t.printStackTrace();
			return null;
		}
	}

//	private static FieldState createInstanceField(Object accessedObj, String fieldName) {
//		try{
//		FieldState fs;
//		int idx = System.identityHashCode(accessedObj) % NUMBER_OF_TABLES;
//		synchronized(instanceFields[idx]) {
//			/** SANITY CHECK **/
//			if(instanceFields[idx].get(accessedObj) != null) {
//				System.err.println("[ImmutabilityAnalysis.createInstanceField] ERROR1!");
//			}
//			/** END_OF: SANITY CHECK **/
//			instanceFields[idx].put(accessedObj, fs = new FieldState(fieldName + " [" + System.identityHashCode(accessedObj) + "]"));
//		}
//		return fs;
//		}
//		catch(Throwable t){
//			t.printStackTrace();
//			return null;
//		}
//	}

//	private static FieldState getInstanceField(Object accessedObj) {
//		FieldState os;
//		int idx = System.identityHashCode(accessedObj) % NUMBER_OF_TABLES;
//		synchronized(instanceFields[idx]) {
//			os = (FieldState)instanceFields[idx].get(accessedObj);
//		}
//		/** SANITY CHECK **/
//		if(os == null) {
//			System.err.println("[ImmutabilityAnalysis.getInstanceField] ERROR2!");
//			System.err.println("OBJ: " + accessedObj.getClass().getName() + "\t" + accessedObj);
//			new Exception().printStackTrace();
//			System.exit(-1);
//		}
//		/** END_OF: SANITY CHECK **/
//		return os;
//	}

	public static boolean isIncluded(Stack<Object> stack, Object accessedObject) {
		for(Iterator<Object> iter = stack.iterator(); iter.hasNext();) {
			if(iter.next() == accessedObject) {
				return true;
			}
		}
		return false;
	}

	private static String getObjectID(Object accessedObj, String allocSite) {
		return System.identityHashCode(accessedObj) + ":" + accessedObj.getClass().getName() + ":" + allocSite;// + ((accessedObj instanceof Thread && allocSite == null) ? ":" + ((Thread)accessedObj).getName() : "");
	}

	private static void dump() {
		try {
			for(int i=0;i<NUMBER_OF_TABLES;i++){
				synchronized(bigMap[i]) {
					bigMap[i].dump();		
				}
			}

		} catch(Throwable e) {
			e.printStackTrace();
		}
	}
}
