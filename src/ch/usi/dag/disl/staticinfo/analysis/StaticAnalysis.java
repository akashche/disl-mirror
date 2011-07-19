package ch.usi.dag.disl.staticinfo.analysis;

public interface StaticAnalysis {

	// It is mandatory to implement this interface
	
	public void setStaticAnalysisInfo(StaticAnalysisInfo sai);
	
	// NOTE: all static analysis methods should follow convention:
	// a) static analysis methods does not have parameters
	// b) return value can be only basic type (+String)
}
