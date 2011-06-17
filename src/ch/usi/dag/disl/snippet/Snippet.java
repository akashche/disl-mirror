package ch.usi.dag.disl.snippet;

import ch.usi.dag.disl.marker.Marker;
import ch.usi.dag.disl.scope.Scope;

public interface Snippet {

	public Marker getMarker();
	
	public Scope getScope();
	
}
