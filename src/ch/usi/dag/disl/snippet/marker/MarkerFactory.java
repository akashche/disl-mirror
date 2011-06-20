package ch.usi.dag.disl.snippet.marker;

import org.objectweb.asm.Type;

public class MarkerFactory {

	public static Marker createMarker(Type marker) {
		try {
			return (Marker) Class.forName(marker.getClassName()).newInstance();
		} catch (Exception e) {
			e.printStackTrace();
			return new EmptyMarker();
		}
	}
}
