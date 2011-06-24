package ch.usi.dag.disl.snippet.marker;

import org.objectweb.asm.Type;

import ch.usi.dag.disl.exception.MarkerException;

public class MarkerFactory {

	public static Marker createMarker(Type marker) throws MarkerException {
		
		try {
			return (Marker) Class.forName(marker.getClassName()).newInstance();
		} catch (Exception e) {
			throw new MarkerException("Marker class " + marker.getClassName()
					+ " cannot be resolved", e);
		}
	}
}
