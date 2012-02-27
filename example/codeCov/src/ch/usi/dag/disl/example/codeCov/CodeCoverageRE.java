package ch.usi.dag.disl.example.codeCov;

import ch.usi.dag.dislre.REDispatch;

public class CodeCoverageRE {
    public static void onConstructor(Class<?> c, int classID) {
        int sid = REDispatch.analysisStart("ch.usi.dag.disl.example.codeCov.CodeCoverage.onConstructor");

        REDispatch.sendClass(sid, c);
        // class_id ignored

        REDispatch.analysisEnd(sid);
    }

    public static void onBB(String fullMethodName, int bbID) {
        int sid = REDispatch.analysisStart("ch.usi.dag.disl.example.codeCov.CodeCoverage.onBB");

        REDispatch.sendString(sid, fullMethodName);
        REDispatch.sendInt(sid, bbID);

        REDispatch.analysisEnd(sid);
    }
}
