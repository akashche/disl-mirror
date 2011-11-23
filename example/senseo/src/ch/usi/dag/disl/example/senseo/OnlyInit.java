package ch.usi.dag.disl.example.senseo;

import ch.usi.dag.disl.annotation.GuardMethod;
import ch.usi.dag.disl.staticcontext.MethodSC;

public class OnlyInit {
    @GuardMethod
    public static boolean isApplicable(MethodSC msc) {
        return (msc.thisMethodName().equals("<init>") && !msc.thisClassName().equals("java/lang/Object")) ? true : false;
    }
}
