package ch.usi.dag.disl.snippet;

import ch.usi.dag.disl.marker.Marker;
import ch.usi.dag.disl.scope.Scope;

public class SnippetImpl {

	protected Marker marker;
	protected Scope scope;
	// TODO protected ?Code? asmCode;

	public Marker getMarker() {
		return marker;
	}
	
	public Scope getScope() {
		return scope;
	}
}
