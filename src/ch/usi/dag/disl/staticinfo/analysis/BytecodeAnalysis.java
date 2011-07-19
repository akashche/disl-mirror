package ch.usi.dag.disl.staticinfo.analysis;

public class BytecodeAnalysis extends AbstractStaticAnalysis {

	public int getBytecodeNumber() {
		
		return staticAnalysisInfo.getMarkedRegion().getStart().getOpcode();
	}
}
