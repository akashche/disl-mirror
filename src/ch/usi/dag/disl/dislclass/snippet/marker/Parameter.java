package ch.usi.dag.disl.dislclass.snippet.marker;

/**
 *  Used for marker parameter parsing
 */
public class Parameter {

	protected String value;
	
	protected String delim;
	
	public Parameter(String value) {
		this.value = value;
	}
	
	public void setMultipleValDelim(String delim) {
		this.delim = delim;
	}
	
	public String getValue() {
		return value;
	}

	public String[] getMultipleValues() {
		return value.split(delim);
	}
}
