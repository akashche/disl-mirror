package ch.usi.dag.disl.test.args;

import org.objectweb.asm.Type;

import ch.usi.dag.disl.guard.SnippetGuard;
import ch.usi.dag.disl.snippet.Shadow;

public class HasArgsGuard  implements SnippetGuard {
	@Override
	public boolean isApplicable(Shadow shadow) {
		
		if(Type.getArgumentTypes(shadow.getMethodNode().desc).length>0)
			return true;
		return false;
	}
}
