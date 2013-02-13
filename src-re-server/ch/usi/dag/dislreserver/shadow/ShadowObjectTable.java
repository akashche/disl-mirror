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

	private static boolean isAssignableFromThread(ShadowClass klass) {

		while ("java.lang.Object".equals(klass.getName())) {

			if ("java.lang.Thread".equals(klass.getName())) {
				return true;
			}

			klass = klass.getSuperclass();
		}

		return false;
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
			ShadowClass klass = ShadowClassTable.get(NetReferenceHelper
					.get_class_id(net_ref));
			ShadowObject tmp = null;

			if ("java.lang.String".equals(klass.getName())) {
				tmp = new ShadowString(net_ref, null, klass);
			} else if (isAssignableFromThread(klass)) {
				tmp = new ShadowThread(net_ref, null, false, klass);
			} else {
				tmp = new ShadowObject(net_ref, klass);
			}

			if ((retVal = shadowObjects.putIfAbsent(objID, tmp)) == null) {
				retVal = tmp;
			}

			return retVal;
		}
	}

	public static void freeShadowObject(ShadowObject obj) {
		shadowObjects.remove(obj.getId());
		ShadowClassTable.freeShadowObject(obj);
	}

	//TODO: find a more elegant way to allow users to traverse the shadow object table
	public static Iterator<Entry<Long, ShadowObject>> getIterator() {
	    return shadowObjects.entrySet().iterator();
	}
}
