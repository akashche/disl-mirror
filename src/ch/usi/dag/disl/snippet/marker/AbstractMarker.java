package ch.usi.dag.disl.snippet.marker;

import ch.usi.dag.disl.util.Parameter;

public abstract class AbstractMarker {

	protected Parameter param;
	
	public void setParam(String paramValue) {
		param = new Parameter(paramValue);
	}
}
