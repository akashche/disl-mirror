package ch.usi.dag.disl.example.senseo;

import ch.usi.dag.disl.annotation.GuardMethod;
import ch.usi.dag.disl.staticcontext.MethodStaticContext;

//check if at least one of the arguments is an Object 
//--> if "L" is in the description of the arguments
public class MethodHasObjectArgs {
    @GuardMethod
    public static boolean isApplicable(MethodStaticContext msc) {
        String desc = msc.thisMethodDescriptor();
        return
            NotInitNorClinit.isApplicable(msc)
            && desc.substring(0, desc.indexOf(')')).contains("L");
    }
}
