package ch.usi.dag.disl.example.fieldsImmutabilityAnalysis.runtime;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Deque;
import java.util.Iterator;
import java.util.zip.GZIPOutputStream;

import ch.usi.dag.jborat.runtime.DynamicBypass;

public class ImmutabilityAnalysis {

	private final MyWeakKeyIdentityHashMap<Object, FieldState[]> fieldStateMap;

	static {
		instanceOfIA = new ImmutabilityAnalysis();
	}

	private static final ImmutabilityAnalysis instanceOfIA;

	private ImmutabilityAnalysis() {
		PrintStream ps = null;
		try {
			String dumpFile = System.getProperty("dump", "dump.tsv.gz");
			ps = new PrintStream(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(dumpFile))));
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(-1);
		}

		final TabSeparatedValuesDumper dumper = new TabSeparatedValuesDumper(ps);
		fieldStateMap = new MyWeakKeyIdentityHashMap<Object, FieldState[]>(dumper);

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				DynamicBypass.set(true);
				System.err.println("In shutdown hook!");
				try {
					synchronized (fieldStateMap) {
						fieldStateMap.dump();
					}
				} catch (Throwable e) {
					e.printStackTrace();
				} finally {
					dumper.close();
				}
			}
		});
	}

	public static ImmutabilityAnalysis instanceOf() {
		return instanceOfIA;
	}

	public void onObjectInitialization(Object allocatedObj, String allocSite) {
		try {
			String objectID = getObjectID(allocatedObj, allocSite);
			FieldState[] fieldsArray = getFieldsArray(allocatedObj);
			if (fieldsArray == null) {
				Offsets.registerIfNeeded(allocatedObj.getClass());
				fieldsArray = getOrCreateFieldsArray(allocatedObj, objectID);
				
				for (Field f: Offsets.getObjectFields(allocatedObj.getClass())) {
					if (!Modifier.isStatic(f.getModifiers())){
						getOrCreateFieldState(allocatedObj, Offsets.getFieldId(allocatedObj.getClass(), f.getName(), f.getType()));
					}
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public void onFieldWrite(Object accessedObj, String fieldId, Deque<Object> stack, String accessSite) {
		try {
			FieldState fs = getOrCreateFieldState(accessedObj, fieldId);
			if(fs != null) {
				fs.onWrite(isUnderConstruction(accessedObj, stack));
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public void onFieldRead(Object accessedObj, String fieldId, Deque<Object> stackTL) {
		try {
			FieldState fs = getOrCreateFieldState(accessedObj, fieldId);
			if(fs != null) {
				fs.onRead();
			}
		} catch(Throwable t) {
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
			if (s != null) {
				synchronized (fieldsArray) {
					if ((fieldState = fieldsArray[s]) == null ){
						fieldState = new FieldState(fieldId);
						fieldsArray[s] = fieldState;
					}
				}
			} else {
				System.err.println("[ImmutabilityAnalysis.getOrCreateFieldState] Warning: unregistered access to: "
						+ fieldId
						+ "; skipping event");
			}
		} catch (Throwable t) {
			t.printStackTrace();
			System.exit(-1);
		}
		return fieldState;
	}

	private FieldState[] getFieldsArray(Object ownerObj) {
		synchronized (fieldStateMap) {
			return fieldStateMap.get(ownerObj);
		}
	}

	private FieldState[] getOrCreateFieldsArray(Object ownerObj, String objectID) {
		FieldState[] fieldsArray;
		synchronized (fieldStateMap) {
			if ((fieldsArray = fieldStateMap.get(ownerObj)) == null) {
				fieldsArray = new FieldState[Offsets.getNumberOfFields(ownerObj.getClass())];
				fieldStateMap.put(ownerObj, fieldsArray, objectID);
			}
		}
		return fieldsArray;
	}

	public boolean isUnderConstruction(Object object, Deque<Object> objectsUnderConstruction) {
		if (objectsUnderConstruction != null) {
			for (Iterator<Object> iter = objectsUnderConstruction.iterator(); iter.hasNext();) {
				if (iter.next() == object) {
					return true;
				}
			}
		}
		return false;
	}

	private String getObjectID(Object accessedObj, String allocSite) {
		return accessedObj.getClass().getName() + ":" + allocSite;
	}

	public void popStackIfNonNull(Deque<Object> stackTL) {
		try {
			stackTL.pop();
		} catch(NullPointerException e) {
			System.err.println("The stack is null " + Thread.currentThread().getName());
		}
	}
}
