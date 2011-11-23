package ch.usi.dag.disl.guard;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ch.usi.dag.disl.annotation.GuardMethod;
import ch.usi.dag.disl.exception.DiSLFatalException;
import ch.usi.dag.disl.exception.GuardException;
import ch.usi.dag.disl.exception.GuardRuntimeException;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.guardcontext.GuardContext;
import ch.usi.dag.disl.processorcontext.ArgumentContext;
import ch.usi.dag.disl.runtimecache.StConCache;
import ch.usi.dag.disl.snippet.Shadow;
import ch.usi.dag.disl.staticcontext.StaticContext;
import ch.usi.dag.disl.util.ReflectionHelper;

public abstract class GuardHelper {

	public static Method findAndValidateGuardMethod(Class<?> guardClass,
			Set<Class<?>> validArgs) throws GuardException {
		
		if(guardClass == null) {
			return null;
		}
		
		Method guardMethod = findGuardMethod(guardClass);
		validateGuardMethod(guardMethod, validArgs);
		return guardMethod;
	}
	
	// TODO ! guard - introduce caching for guardClass -> guardMethod using singleton hash map
	// GuardMethod will not contain only method object but also all contexts set - for quick validation
	
	private static Method findGuardMethod(Class<?> guardClass)
			throws GuardException {
		
		Method guardMethod = null;
		
		// check all methods
		for(Method method : guardClass.getMethods()) {
			
			if(method.isAnnotationPresent(GuardMethod.class)) {
				
				// detect multiple annotations
				if(guardMethod != null) {
					throw new GuardException("Detected several "
							+ GuardMethod.class.getName()
							+ " annotations on guard class "
							+ guardClass.getName());
				}
				
				guardMethod = method;
			}
		}
		
		// detect no annotation
		if(guardMethod == null) {
			throw new GuardException("No "
					+ GuardMethod.class.getName()
					+ " annotation on guard class "
					+ guardClass.getName());
		}
		
		return guardMethod;
	}
	
	private static void validateGuardMethod(Method guardMethod,
			Set<Class<?>> validArgs) throws GuardException {
		
		String guardMethodName = guardMethod.getDeclaringClass().getName()
				+ "." + guardMethod.getName();
		
		if(! guardMethod.getReturnType().equals(boolean.class)) {
			throw new GuardException("Guard method " + guardMethodName
					+ " should return boolean type");
		}
		
		if(! Modifier.isStatic(guardMethod.getModifiers())) {
			throw new GuardException("Guard method " + guardMethodName
					+ " should be static");
		}
		
		// for all arguments
		for(Class<?> argType : guardMethod.getParameterTypes()) {
		
			// throws exception in the case of invalidity
			validateArgument(guardMethodName, argType, validArgs);
		}
	}
	
	private static void validateArgument(String guardMethodName,
			Class<?> argClass, Set<Class<?>> validArgClasses)
			throws GuardException {
	
		// validate that implements one of the allowed interfaces
		for(Class<?> allowedInterface : validArgClasses) {

			// valid
			if(argClass.equals(allowedInterface)) {
				return;
			}
			
			// valid - note that static context has to be implemented
			if(allowedInterface.equals(StaticContext.class) && ReflectionHelper
					.implementsInterface(argClass, allowedInterface)) {
				return;
			}
		}
		
		// construct the error massage
		StringBuilder sb = new StringBuilder("Guard argument "
				+ argClass.getName()
				+ " in " + guardMethodName
				+ " does not implement any of the allowed interfaces: ");
		
		for(Class<?> allowedInterface : validArgClasses) {

			sb.append(allowedInterface.getName() + ", ");
		}
		
		throw new GuardException(sb.toString());
	}

	// *** Methods tight with processor or snippet guard ***

	public static Set<Class<?>> snippetContextSet() {

		Set<Class<?>> allowedSet = new HashSet<Class<?>>();

		allowedSet.add(GuardContext.class);
		allowedSet.add(StaticContext.class);

		return allowedSet;
	}
	
	public static Set<Class<?>> processorContextSet() {
		
		Set<Class<?>> allowedSet = new HashSet<Class<?>>();
		
		allowedSet.add(GuardContext.class);
		allowedSet.add(StaticContext.class);
		allowedSet.add(ArgumentContext.class);
		
		return allowedSet;
	}
	
	// invoke guard method for snippet guard
	public static boolean guardApplicable(Method guardMethod, Shadow shadow) {
		
		if(guardMethod == null) {
			return true;
		}
		
		// no method validation needed - already validated
		return invokeGuardMethod(guardMethod, shadow, null);
	}
	
	// invoke guard method for processor guard
	public static boolean guardApplicable(Method guardMethod, Shadow shadow,
			int position, String typeDescriptor, int totalCount) {
		
		if(guardMethod == null) {
			return true;
		}
		
		// no method validation needed - already validated
		return invokeGuardMethod(guardMethod, shadow, 
				new ArgumentContextImpl(position, typeDescriptor, totalCount));
	}

	// invoke guard for processor or snippet guard
	// this is just helper method for GuardContextImpl - reduced visibility
	static boolean invokeGuard(Class<?> guardClass, Shadow shadow,
			ArgumentContext ac) throws GuardException {
		
		// find and validate method first
		Method guardMethod = findGuardMethod(guardClass);

		Set<Class<?>> validationSet;

		if (ac == null) {
			// no argument context supplied -> snippet guard
			validationSet = snippetContextSet();
		} else {
			// argument context supplied -> processor guard
			validationSet = processorContextSet();
		}

		validateGuardMethod(guardMethod, validationSet);

		return invokeGuardMethod(guardMethod, shadow, ac);
		
	}
	
	// invoke guard method for processor or snippet guard
	// NOTE: all calling methods should guarantee using validation method,
	// that if ArgumentContext is needed, it cannot be null
	private static boolean invokeGuardMethod(Method guardMethod, Shadow shadow,
			ArgumentContext ac) {
		
		List<Object> argumentInstances = new LinkedList<Object>();
		
		String guardMethodName = guardMethod.getDeclaringClass().getName()
				+ "." + guardMethod.getName();
		
		for(Class<?> argType : guardMethod.getParameterTypes()) {
			
			// argument context
			if(argType.equals(ArgumentContext.class)) {
				
				if(ac == null) {
					throw new DiSLFatalException(
							"Argument context is reguired but not supplied");
				}
				
				argumentInstances.add(ac);
				
				continue;
			}
			
			// guard context
			if(argType.equals(GuardContext.class)) {
				
				argumentInstances.add(new GuardContextImpl(shadow, ac));
				
				continue;
			}
			
			// static context
			// if it passes validation it can be only static context here
			try {
				
				// get static context
				StaticContext scInst = StConCache.getInstance()
						.getStaticContextInstance(argType);
				
				// populate with data
				scInst.staticContextData(shadow);
				
				argumentInstances.add(scInst);
			} catch (ReflectionException e) {
				throw new GuardRuntimeException(
						"Static context initialization for guard "
						+ guardMethodName
						+ " failed", e);
			}
		}
		
		try {
			
			// invoke guard method
			
			Object retVal = guardMethod.invoke(null,
					argumentInstances.toArray());
			
			return (Boolean)retVal;
			
		} catch (Exception e) {
			throw new GuardRuntimeException("Invocation of guard method"
					+ guardMethodName + " failed", e);
		}
	}
}
