package ch.usi.dag.disl.test.args;

import org.objectweb.asm.Type;
import ch.usi.dag.disl.staticcontext.AbstractStaticContext;

public class ArgsAnalysis extends AbstractStaticContext {

	public int getNumberOfArgs() {
		return  Type.getArgumentTypes(staticContextData.getMethodNode().desc).length; 
	}

}
