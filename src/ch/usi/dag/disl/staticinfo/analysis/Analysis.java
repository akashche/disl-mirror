package ch.usi.dag.disl.staticinfo.analysis;

public interface Analysis {

	// NOTE: all classes implementing this iface should follow convention:
	// a) method returns the desired value as the method return value
	// (not by using modified argument)
	// b) return value can be only basic type (+String)
	// c) method gets only one parameter -> StaticInfoData
	// d) methods should be static
	// e) all classes should share the prefix of the package with this interface
	// f) javadoc should explain, that the parameter passed by user should be
	// null (because it will be invoked by DiSL with proper parameter)
}
