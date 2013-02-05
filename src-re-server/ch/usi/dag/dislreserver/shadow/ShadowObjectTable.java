package ch.usi.dag.dislreserver.shadow;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import ch.usi.dag.dislreserver.exception.DiSLREServerFatalException;

public class ShadowObjectTable {

	private static ConcurrentHashMap<Long, ShadowObject> shadowObjects;

	static {
		shadowObjects = new ConcurrentHashMap<Long, ShadowObject>();
	}

	public static void register(ShadowObject obj, boolean debug) {

		if (obj == null) {
			throw new DiSLREServerFatalException(
					"Attempting to register a null as a shadow object");
		}

		long objID = obj.getId();
		ShadowObject exist = shadowObjects.putIfAbsent(objID, obj);

		if (exist != null) {
			if (exist.equals(obj)) {
				if (debug) {
					System.out.println("Re-register a shadow object.");
				}
			} else {
				throw new DiSLREServerFatalException("Duplicated net reference");
			}
		}
	}

	public static ShadowObject get(long net_ref) {

		long objID = NetReferenceHelper.get_object_id(net_ref);

		if (objID == 0) {
			// reserved ID for null
			return null;
		}

		ShadowObject retVal = shadowObjects.get(objID);

		if (retVal != null) {
			return retVal;
		}

		if (NetReferenceHelper.isClassInstance(objID)) {
			throw new DiSLREServerFatalException("Unknown class instance");
		} else {
			// Only common shadow object will be generated here
			ShadowObject tmp = new ShadowObject(net_ref);

			if ((retVal = shadowObjects.putIfAbsent(objID, tmp)) == null) {
				retVal = tmp;
			}

			return retVal;
		}
	}

	public static void freeShadowObject(long net_ref, ShadowObject obj) {
		ShadowClassTable.freeShadowObject(net_ref, obj);
		shadowObjects.remove(obj.getId());
	}

	//TODO: find a more elegant way to allow users to traverse the shadow object table
	public static Iterator<Entry<Long, ShadowObject>> getIterator() {
	    return shadowObjects.entrySet().iterator();
	}
}
