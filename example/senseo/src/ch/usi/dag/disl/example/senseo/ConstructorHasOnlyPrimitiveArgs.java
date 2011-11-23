package ch.usi.dag.disl.example.senseo;

import ch.usi.dag.disl.annotation.GuardMethod;
import ch.usi.dag.disl.staticcontext.MethodSC;

//check if none of the arguments is an Object 
//--> if "L" is not in the description of the arguments
public class ConstructorHasOnlyPrimitiveArgs {
    @GuardMethod
    public static boolean isApplicable(MethodSC msc) {
        String desc = msc.thisMethodDescriptor();
        return
            OnlyInit.isApplicable(msc)
            && !desc.substring(0, desc.indexOf(')')).contains("L");
    }
}
