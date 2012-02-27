package ch.usi.dag.disl.example.codeCov;

import ch.usi.dag.dislre.REDispatch;

public class CodeCoverageRE {
    public static void onConstructor(Class<?> c) {
        int sid = REDispatch.analysisStart(5);

        REDispatch.sendClass(sid, c);

        REDispatch.analysisEnd(sid);
    }

    public static void onBB(String fullMethodName, int bbID) {
        int sid = REDispatch.analysisStart(6);

        REDispatch.sendString(sid, fullMethodName);
        REDispatch.sendInt(sid, bbID);

        REDispatch.analysisEnd(sid);
    }
}
