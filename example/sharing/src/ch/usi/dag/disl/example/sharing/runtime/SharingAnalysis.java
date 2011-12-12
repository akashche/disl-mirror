package ch.usi.dag.disl.example.sharing.runtime;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;

import ch.usi.dag.jborat.runtime.DynamicBypass;

public class SharingAnalysis {
	public static final boolean DEBUG = Boolean.getBoolean("ch.usi.dag.example.sharing.debug");

	private final MyWeakKeyIdentityHashMap<Object, FieldState[]> fieldStateMap;
	private static final SharingAnalysis instanceOfSA;

	static {
		instanceOfSA = new SharingAnalysis();
	}

	private SharingAnalysis() {
		PrintStream ps = null;
		try{
			String dumpFile = System.getProperty("dump", "dump.log");
			ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(dumpFile)));
		}
		catch(Throwable t){
			t.printStackTrace();
			System.exit(-1);
		}

		final MyDumper myDumper = new MyDumper(ps);
		fieldStateMap = new MyWeakKeyIdentityHashMap<Object, FieldState[]>(myDumper);

		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run() {
				DynamicBypass.set(true);
				System.err.println("In shutdown hook!");
				try {
					synchronized(fieldStateMap) {
						fieldStateMap.dump();
					}
				} catch(Throwable e) {
					e.printStackTrace();
				} finally {
					myDumper.close();
				}
			}
		});
	}

	public static SharingAnalysis instanceOf() {
		return instanceOfSA;
	}

	public  void onObjectInitialization(Object allocatedObj, String allocSite) {
		try {
			String objectID = getObjectID(allocatedObj, allocSite);
			FieldState[] fieldsArray = getFieldsArray(allocatedObj);
			if(fieldsArray == null) {
				Offsets.registerIfNeeded(allocatedObj.getClass());
				fieldsArray = getOrCreateFieldsArray(allocatedObj, objectID);
			}
		}
		catch(Throwable t) {
			t.printStackTrace();
		}
	}

	public void onFieldWrite(Object accessedObj, String fieldId) {
		try {
			FieldState fs = getOrCreateFieldState(accessedObj, fieldId);
			if(fs != null) {
				fs.onWrite(Thread.currentThread());
			}
		}
		catch(Throwable t) {
			t.printStackTrace();
		}
	}

	public void onFieldRead(Object accessedObj, String fieldId) {
		try {
			FieldState fs = getOrCreateFieldState(accessedObj, fieldId);
			if(fs != null) {
				fs.onRead(Thread.currentThread());
			}
		}
		catch(Throwable t) {
			t.printStackTrace();
		}
	}

	private FieldState getOrCreateFieldState(Object accessedObj, String fieldId) {
		FieldState fieldState = null;
		try {
			String objectID = getObjectID(accessedObj, null);
			FieldState[] fieldsArray = getFieldsArray(accessedObj);

			if(fieldsArray == null) {
				Offsets.registerIfNeeded(accessedObj.getClass());
				fieldsArray = getOrCreateFieldsArray(accessedObj, objectID);
			}
			Short s = Offsets.getFieldOffset(fieldId); 
			if(s != null) {
				synchronized (fieldsArray) {
					if ((fieldState = fieldsArray[s]) == null ){
						fieldState = new FieldState(fieldId);
						fieldsArray[s] = fieldState;
					}
				}
			}
			else {
				System.err.println("[SharingAnalysis.getOrCreateFieldState] Warning: unregistered access to: "
						+ fieldId
						+ "; skipping event");
			}
		}
		catch(Throwable t){
			t.printStackTrace();
			System.exit(-1);
		}
		return fieldState;
	}

	private FieldState[] getFieldsArray(Object ownerObj) {
		try{
		synchronized (fieldStateMap) {
			return fieldStateMap.get(ownerObj);
		}
		} catch(Throwable t) {
			t.printStackTrace();
			return null;
		}

	}

	private FieldState[] getOrCreateFieldsArray(Object ownerObj, String objectID) {
		FieldState[] fieldsArray;
		synchronized (fieldStateMap) {
			if ((fieldsArray = fieldStateMap.get(ownerObj)) == null) {
				fieldStateMap.put(ownerObj, fieldsArray =  new FieldState[Offsets.getNumberOfFields(ownerObj.getClass())], objectID);
			}
		}
		return fieldsArray;
	}

	private static String getObjectID(Object accessedObj, String allocSite) {
		return accessedObj.getClass().getName() + ":" + allocSite;
	}
}
