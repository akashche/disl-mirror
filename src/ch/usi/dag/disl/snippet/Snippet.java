package ch.usi.dag.disl.snippet;

import ch.usi.dag.disl.snippet.marker.Marker;
import ch.usi.dag.disl.snippet.scope.Scope;

public interface Snippet {

	public Marker getMarker();
	
	public Scope getScope();
	
}
