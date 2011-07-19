package ch.usi.dag.disl.staticinfo.analysis;

public interface Analysis {

	// It is mandatory to implement this interface
	
	public void setStaticAnalysisInfo(AnalysisInfo ai);
	
	// NOTE: all analysis methods should follow convention:
	// a) analysis methods does not have parameters
	// b) return value can be only basic type (+String)
}
