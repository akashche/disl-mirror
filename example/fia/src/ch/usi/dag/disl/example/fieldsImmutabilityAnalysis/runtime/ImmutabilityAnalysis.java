package ch.usi.dag.disl.example.fieldsImmutabilityAnalysis.runtime;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Stack;

import ch.usi.dag.jborat.runtime.DynamicBypass;



@SuppressWarnings("unchecked")
public class ImmutabilityAnalysis {

	private static MyWeakKeyIdentityHashMap<Object,FieldStateList> bigMap;
	
	private static final String DUMP_FILE = System.getProperty("dump", "dump.log");

	private static MyDumper myDumper;
	
	
	static {
		boolean oldState = DynamicBypass.getAndSet();
		try{
			PrintStream ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(DUMP_FILE)));
			myDumper = new MyDumper(ps);
			bigMap = new MyWeakKeyIdentityHashMap(myDumper);

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

	public static void onFieldRead(Object accessedObj, String accessedFieldName ) {
		try{
			
			String objectID = getObjectID(accessedObj, null);
			FieldState fs = getOrCreateInstanceField(accessedObj, objectID, accessedFieldName);
			fs.onRead();

		}
		catch(Throwable t){
			t.printStackTrace();
		}
	}

	public static void onObjectInitialization(Object allocatedObj, String allocSite) {
		try{
			String objectID = getObjectID(allocatedObj, allocSite);
			synchronized (bigMap) {
				if(bigMap.get(allocatedObj) == null) {
					bigMap.put(allocatedObj, new FieldStateList(),objectID);
				}	
				else{
					System.err.println("object is already  in the table!");
				}
			}
		}
		catch(Throwable t){
			t.printStackTrace();
		}
	}


	public static void onFieldWrite(Object accessedObj, String accessedFieldName, Stack<Object> stack) {
		try{
			boolean  isInDynamicExtendOfConstructor = false;
			if(stack != null) {
				if(isIncluded(stack, accessedObj)) {
					isInDynamicExtendOfConstructor = true;
				}
			}
			String objectID = getObjectID(accessedObj, null);
			FieldState fs = getOrCreateInstanceField(accessedObj, objectID, accessedFieldName);
			fs.onWrite(isInDynamicExtendOfConstructor);
		}
		catch(Throwable t){
			t.printStackTrace();
		}
	}

	private static FieldState getOrCreateInstanceField(Object accessedObj, String objectId, String fieldName) {
		try{
			FieldStateList smallMap;
			FieldState fs = null;

			synchronized(bigMap){
				if((smallMap = bigMap.get(accessedObj)) == null) {
					bigMap.put(accessedObj, smallMap = new FieldStateList(), objectId);
				}
			}

			synchronized(smallMap) {
				if((fs = smallMap.get(fieldName)) == null) {
					fs = smallMap.put(fieldName);
				}
			}

			return fs;
		}
		catch(Throwable t){
			t.printStackTrace();
			return null;
		}
	}



	public static boolean isIncluded(Stack<Object> stack, Object accessedObject) {
		for(Iterator<Object> iter = stack.iterator(); iter.hasNext();) {
			if(iter.next() == accessedObject) {
				return true;
			}
		}
		return false;
	}

	private static String getObjectID(Object accessedObj, String allocSite) {
		return System.identityHashCode(accessedObj) + ":" + accessedObj.getClass().getName() + ":" + allocSite;
	}

	private static void dump() {
		try {
			synchronized(bigMap) {
				bigMap.dump();		
			}
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}
}
