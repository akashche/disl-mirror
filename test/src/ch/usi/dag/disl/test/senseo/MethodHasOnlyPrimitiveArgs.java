package ch.usi.dag.disl.test.senseo;

import ch.usi.dag.disl.snippet.Shadow;

// check if none of the arguments is an Object 
// --> if "L" is not in the description of the arguments
public class MethodHasOnlyPrimitiveArgs extends NotInitNorClinit {
    @Override
    public boolean isApplicable(Shadow shadow) {
        String desc = shadow.getMethodNode().desc;
        return
            super.isApplicable(shadow)
            && !desc.substring(0, desc.indexOf(')')).contains("L");
    }
}
