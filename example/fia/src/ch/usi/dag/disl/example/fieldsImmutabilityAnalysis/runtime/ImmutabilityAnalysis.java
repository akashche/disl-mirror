package ch.usi.dag.disl.example.fieldsImmutabilityAnalysis.runtime;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLongArray;

import ch.usi.dag.jborat.runtime.DynamicBypass;

public class ImmutabilityAnalysis {
	private static final ImmutabilityAnalysis instanceOfIA;

	private final String dumpFile;

	private final MyWeakKeyIdentityHashMap<Object, AtomicLongArray> fields;
	private final MyDumper myDumper;

	static {
		instanceOfIA = new ImmutabilityAnalysis();
	}

	private ImmutabilityAnalysis() {
		dumpFile = System.getProperty("dump", "dump.log");

		PrintStream ps = null;
		try{
			ps = new PrintStream(new BufferedOutputStream(new FileOutputStream(dumpFile)));
		}
		catch(Throwable t){
			t.printStackTrace();
			System.exit(-1);
		}

		myDumper = new MyDumper(ps);
		fields = new MyWeakKeyIdentityHashMap<Object, AtomicLongArray>(myDumper);

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

	public static ImmutabilityAnalysis instanceOf() {
		return instanceOfIA;
	}

	public void onObjectInitialization(Object allocatedObj, String allocSite) {
		try {
			String objectID = getObjectID(allocatedObj, allocSite);
			AtomicLongArray fieldsArray = getFieldsArray(allocatedObj, objectID);
			if(fieldsArray == null) {
				Offsets.registerIfNeeded(allocatedObj.getClass());
				fieldsArray = getOrCreateFieldsArray(allocatedObj, objectID);
			}
		}
		catch(Throwable t) {
			t.printStackTrace();
		}
	}

	public void onFieldWrite(Object accessedObj, String accessedFieldId, Deque<Object> stack) {
		try {
			if(isInDynamicExtendOfConstructor(stack, accessedObj)) {
				updateFieldsArray(accessedObj, accessedFieldId);
			}
		}
		catch(Throwable t){
			t.printStackTrace();
		}
	}

	private void updateFieldsArray(Object accessedObj, String fieldName) {
		try {
			String objectID = getObjectID(accessedObj, null);
			AtomicLongArray fieldsArray = getFieldsArray(accessedObj, objectID);
			if(fieldsArray == null) {
				Offsets.registerIfNeeded(accessedObj.getClass());
				fieldsArray = getOrCreateFieldsArray(accessedObj, objectID);
			}
			Short s = Offsets.getFieldOffset(fieldName); 
			if(s != null) {
				fieldsArray.incrementAndGet(s);
			}
			else {
				System.err.println("[ImmutabilityAnalysis.updateFieldsArray] Warning: unregistered access to: "
						+ fieldName
						+ "; skipping event");
			}
		}
		catch(Throwable t){
			t.printStackTrace();
			System.exit(-1);
		}
	}

	private AtomicLongArray getFieldsArray(Object ownerObj, String objectID ) {
		synchronized (fields) {
			return fields.get(ownerObj);
		}
	}

	private AtomicLongArray getOrCreateFieldsArray(Object ownerObj, String objectID) {
		AtomicLongArray fieldsArray;
		synchronized (fields) {
			if ((fieldsArray = fields.get(ownerObj)) == null) {
				fields.put(ownerObj, fieldsArray =  new AtomicLongArray(Offsets.getNumberOfFields(ownerObj.getClass())), objectID);
			}
		}
		return fieldsArray;
	}

	public boolean isInDynamicExtendOfConstructor(Deque<Object> stack, Object accessedObject) {
		if(stack != null) {
			for(Iterator<Object> iter = stack.iterator(); iter.hasNext();) {
				if(iter.next() == accessedObject) {
					return true;
				}
			}
		}
		return false;
	}

	private String getObjectID(Object accessedObj, String allocSite) {
		return accessedObj.getClass().getName() + ":" + allocSite;
	}

	private void dump() {
		try {
			synchronized(fields) {
				fields.dump();
			}
		} catch(Throwable e) {
			e.printStackTrace();
		}
	}

	public void popStackIfNonNull(Deque<Object> stackTL) {
		try {
			stackTL.pop();
		} catch(NullPointerException e) {
			System.err.println("The stack is null " + Thread.currentThread().getName());
		}
	}
}
