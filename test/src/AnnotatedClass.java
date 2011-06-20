import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.snippet.marker.BodyMarker;


public class AnnotatedClass {
	@Before(marker = BodyMarker.class, scope = "TargetClass.print()")
	public static void precondition() {
		System.out.println("Precondition!");
	}
	
	@After(marker = BodyMarker.class, scope = "TargetClass.print()")
	public static void postcondition() {
		System.out.println("Postcondition!");
	}
}
