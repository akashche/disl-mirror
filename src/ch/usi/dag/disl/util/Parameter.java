package ch.usi.dag.disl.util;

public class Parameter {

	protected String name;

	protected String value;
	
	Parameter(String param) {
		// TODO parse parameter
	}
	
	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}
	
	// TODO add methods getValueAs ... Int, Float, Boolean,...
}
