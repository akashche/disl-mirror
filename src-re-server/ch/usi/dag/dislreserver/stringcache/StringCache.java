package ch.usi.dag.dislreserver.stringcache;

import java.util.HashMap;
import java.util.Map;

import ch.usi.dag.dislreserver.exception.DiSLREServerFatalException;

public class StringCache {

	private static Map<Long, String> cache = new HashMap<Long, String>();
	
	public static void register(long strId, String str) {

		cache.put(strId, str);
	}

	public static String resolve(long strId) {

		String str = cache.get(strId);
		
		if(str == null) {
			throw new DiSLREServerFatalException("String not found in cache");
		}
		
		return str;
	}
}
