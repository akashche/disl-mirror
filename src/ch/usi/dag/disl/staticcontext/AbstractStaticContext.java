package ch.usi.dag.disl.staticcontext;

import ch.usi.dag.disl.snippet.Shadow;

public abstract class AbstractStaticContext implements StaticContext {

	protected Shadow staticContextData;

	public void staticContextData(Shadow sa) {

		staticContextData = sa;
	}

}
