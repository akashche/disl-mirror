package ch.usi.dag.disl.test.args;

import org.objectweb.asm.Type;

import ch.usi.dag.disl.annotation.GuardMethod;
import ch.usi.dag.disl.staticcontext.MethodSC;

public class HasArgsGuard {
	
	@GuardMethod
	public static boolean isApplicable(MethodSC msc) {
		
		if(Type.getArgumentTypes(msc.thisMethodDescriptor()).length>0)
			return true;
		return false;
	}
}
