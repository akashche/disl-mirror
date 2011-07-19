package ch.usi.dag.disl.staticinfo.analysis;

abstract public class AbstractStaticAnalysis implements StaticAnalysis {

	protected StaticAnalysisInfo staticAnalysisInfo;
	
	@Override
	public void setStaticAnalysisInfo(StaticAnalysisInfo sai) {
		
		// TODO ! analysis - implement caching
		staticAnalysisInfo = sai;
	}

}
