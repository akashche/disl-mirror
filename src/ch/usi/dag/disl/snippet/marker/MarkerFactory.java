package ch.usi.dag.disl.snippet.marker;

import org.objectweb.asm.Type;

public class MarkerFactory {

	public static Marker createMarker(Type marker) {
		try {
			return (Marker) Class.forName(marker.getClassName()).newInstance();
		} catch (Exception e) {
			// TODO report user errors
			e.printStackTrace();
			// TODO continue with empty marker ??
			return new EmptyMarker();
		}
	}
}
