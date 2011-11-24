package ch.usi.dag.disl.resolver;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import ch.usi.dag.disl.exception.GuardException;

public class GuardResolver {


	private static GuardResolver instance = null;
	
	// Guard to guard method map
	private Map<Class<?>, GuardMethod> guardToMethod =
			new HashMap<Class<?>, GuardMethod>();
	
	public synchronized GuardMethod getGuardMethod(
			Class<?> guardClass) throws GuardException {
		
		GuardMethod guardMethod = guardToMethod.get(guardClass);
		
		// resolved from cache
		if(guardMethod != null) {
			return guardMethod;
		}
		
		// no cache hit
		
		// check all methods
		for(Method method : guardClass.getMethods()) {
			
			if(method.isAnnotationPresent(
					ch.usi.dag.disl.annotation.GuardMethod.class)) {
				
				// detect multiple annotations
				if(guardMethod != null) {
					throw new GuardException("Detected several "
							+ GuardMethod.class.getName()
							+ " annotations on guard class "
							+ guardClass.getName());
				}
				
				guardMethod = new GuardMethod(method);
			}
		}
		
		// detect no annotation
		if(guardMethod == null) {
			throw new GuardException("No "
					+ ch.usi.dag.disl.annotation.GuardMethod.class.getName()
					+ " annotation on guard class "
					+ guardClass.getName());
		}
		
		// put into cache
		guardToMethod.put(guardClass, guardMethod);
		
		return guardMethod;
	}
	
	public static synchronized GuardResolver getInstance() {
		
		if (instance == null) {
			instance = new GuardResolver();
		}
		return instance;
	}
}
