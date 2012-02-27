package ch.usi.dag.disl.example.codeCov;

import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.marker.BasicBlockMarker;
import ch.usi.dag.disl.staticcontext.BasicBlockStaticContext;
import ch.usi.dag.disl.staticcontext.MethodStaticContext;

public class DiSLClassBBs {
    @Before(marker = BasicBlockMarker.class, scope = "TargetClass.*")
    public static void beforeBB(MethodStaticContext msc, BasicBlockStaticContext bbsc) {
//        System.out.println("[CLIENT] Before bb");
        CodeCoverageRE.onBB(msc.thisMethodFullName(), bbsc.getBBindex());
    }
}
