package ch.usi.dag.disl.staticinfo.analysis;

abstract public class AbstractStaticAnalysis implements StaticAnalysis {

	protected StaticAnalysisInfo staticAnalysisInfo;
	
	@Override
	public Object setStaticAnalysisInfo(StaticAnalysisInfo sai) {
		
		staticAnalysisInfo = sai;

		// TODO ! analysis - implement caching
		return null;
	}

}
