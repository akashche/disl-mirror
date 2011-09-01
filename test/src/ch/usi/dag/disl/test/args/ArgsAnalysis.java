package ch.usi.dag.disl.test.args;

import org.objectweb.asm.Type;
import ch.usi.dag.disl.staticinfo.analysis.AbstractStaticAnalysis;

public class ArgsAnalysis extends AbstractStaticAnalysis {

	public int getNumberOfArgs() {
		return  Type.getArgumentTypes(staticAnalysisData.getMethodNode().desc).length; 
	}

}
