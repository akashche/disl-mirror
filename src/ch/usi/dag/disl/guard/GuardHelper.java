package ch.usi.dag.disl.guard;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ch.usi.dag.disl.exception.DiSLFatalException;
import ch.usi.dag.disl.exception.GuardException;
import ch.usi.dag.disl.exception.GuardRuntimeException;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.guardcontext.GuardContext;
import ch.usi.dag.disl.processorcontext.ArgumentContext;
import ch.usi.dag.disl.resolver.GuardMethod;
import ch.usi.dag.disl.resolver.GuardResolver;
import ch.usi.dag.disl.resolver.SCResolver;
import ch.usi.dag.disl.snippet.Shadow;
import ch.usi.dag.disl.staticcontext.StaticContext;
import ch.usi.dag.disl.util.ReflectionHelper;

public abstract class GuardHelper {

	public static Method findAndValidateGuardMethod(Class<?> guardClass,
			Set<Class<?>> validArgs) throws GuardException {
		
		if(guardClass == null) {
			return null;
		}
		
		GuardMethod guardMethod = 
				GuardResolver.getInstance().getGuardMethod(guardClass);
		validateGuardMethod(guardMethod, validArgs);
		return guardMethod.getMethod();
	}
	
	private static void validateGuardMethod(GuardMethod guardMethod,
			Set<Class<?>> validArgs) throws GuardException {
		
		// quick validation
		if(guardMethod.getArgTypes() != null) {
			
			// only valid argument types are in the method - ok 
			if(validArgs.containsAll(guardMethod.getArgTypes())) {
				return;
			}
			
			// we have some additional argument types then only valid ones 
			
			// prepare invalid argument type set 
			Set<Class<?>> invalidArgTypes = 
					new HashSet<Class<?>>(guardMethod.getArgTypes());
			invalidArgTypes.removeAll(validArgs);
			
			// construct the error massage
			throw new GuardException("Guard "
					+ guardMethod.getMethod().getDeclaringClass().getName()
					+ " is using interface "
					+ invalidArgTypes.iterator().next().getName()
					+ " not allowed in this particular case (misused guard??)");
		}
		
		// validate properly
		Method method = guardMethod.getMethod(); 
		
		String guardMethodName = method.getDeclaringClass().getName()
				+ "." + method.getName();
		
		if(! method.getReturnType().equals(boolean.class)) {
			throw new GuardException("Guard method " + guardMethodName
					+ " should return boolean type");
		}
		
		if(! Modifier.isStatic(method.getModifiers())) {
			throw new GuardException("Guard method " + guardMethodName
					+ " should be static");
		}
		
		// remember argument types for quick validation
		Set<Class<?>> argTypes = new HashSet<Class<?>>();
		
		// for all arguments
		for(Class<?> argType : method.getParameterTypes()) {
		
			// throws exception in the case of invalidity
			argTypes.add(validateArgument(guardMethodName, argType, validArgs));
		}
		
		// set argument types for quick validation
		guardMethod.setArgTypes(argTypes);
	}
	
	private static Class<?> validateArgument(String guardMethodName,
			Class<?> argClass, Set<Class<?>> validArgClasses)
			throws GuardException {
	
		// validate that implements one of the allowed interfaces
		for(Class<?> allowedInterface : validArgClasses) {

			// valid
			if(argClass.equals(allowedInterface)) {
				return allowedInterface;
			}
			
			// valid - note that static context has to be implemented
			if(allowedInterface.equals(StaticContext.class) && ReflectionHelper
					.implementsInterface(argClass, allowedInterface)) {
				return allowedInterface;
			}
		}
		
		// construct the error massage
		StringBuilder sb = new StringBuilder("Guard argument "
				+ argClass.getName()
				+ " in " + guardMethodName
				+ " is not in the set of allowed interfaces"
				+ " (misused guard??): ");
		
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
		GuardMethod guardMethod = 
				GuardResolver.getInstance().getGuardMethod(guardClass);

		Set<Class<?>> validationSet;

		if (ac == null) {
			// no argument context supplied -> snippet guard
			validationSet = snippetContextSet();
		} else {
			// argument context supplied -> processor guard
			validationSet = processorContextSet();
		}

		validateGuardMethod(guardMethod, validationSet);

		// invoke method
		return invokeGuardMethod(guardMethod.getMethod(), shadow, ac);
		
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
				StaticContext scInst = SCResolver.getInstance()
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
