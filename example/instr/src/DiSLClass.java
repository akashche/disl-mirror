import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.AfterReturning;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.marker.BodyMarker;

public class DiSLClass {
	
	@Before(marker = BodyMarker.class, scope = "Main.main")
	public static void beforemain() {
		
		System.out.println("Before main()");
	}
	
	@After(marker = BodyMarker.class, scope = "Main.main")
	public static void aftermain() {
		System.out.println("After main()");
	}
}
