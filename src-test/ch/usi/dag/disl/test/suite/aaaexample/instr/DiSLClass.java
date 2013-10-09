package ch.usi.dag.disl.test.suite.aaaexample.instr;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.marker.BodyMarker;

public class DiSLClass {

    @After(marker = BodyMarker.class, scope = "TargetClass.main")
    public static void after() {
        System.out.println("disl: after");
    }
}
