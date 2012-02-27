package ch.usi.dag.disl.example.codeCov;

import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.classcontext.ClassContext;
import ch.usi.dag.disl.marker.BodyMarker;
import ch.usi.dag.disl.staticcontext.MethodStaticContext;

public class DiSLClassConstructors {
    @Before(marker = BodyMarker.class, guard = OnlyInit.class, scope = "TargetClass.*")
    public static void beforeConstructor(ClassContext cc, MethodStaticContext msc) {
//        System.out.println("[CLIENT] Before constructor");
        CodeCoverageRE.onConstructor(cc.asClass(msc.thisClassName()), 0);
    }
}
