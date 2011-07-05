package ch.usi.dag.disl.staticinfo.analysis;

public interface Analysis {

	// It isn't mandatory (but it's good practice) to implement this interface
	
	// NOTE: all analysis classes should follow convention:
	// a) method returns the desired value as the method return value
	// (not by using modified argument)
	// b) return value can be only basic type (+String)
	// c) method gets only one parameter -> AnalysisInfo
	// d) methods should be static
	// e) all classes should share the prefix of the package with this interface
	// f) javadoc should explain, that the parameter passed by user should be
	// null (because it will be invoked by DiSL with proper parameter)
}
