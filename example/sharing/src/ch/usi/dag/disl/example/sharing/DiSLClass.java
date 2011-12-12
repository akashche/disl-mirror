package ch.usi.dag.disl.example.sharing;

import ch.usi.dag.disl.annotation.AfterReturning;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.dynamiccontext.DynamicContext;
import ch.usi.dag.disl.example.sharing.runtime.SharingAnalysis;
import ch.usi.dag.disl.marker.BytecodeMarker;
import ch.usi.dag.disl.staticcontext.MethodStaticContext;

public class DiSLClass {
    /** ALLOCATION SITE **/
    @AfterReturning(marker = BytecodeMarker.class, args = "new")
    public static void beforeInitialization(MyMethodStaticContext ma, DynamicContext dc) {
        SharingAnalysis.instanceOf().onObjectInitialization(
                dc.getStackValue(0, Object.class), //allocated object
                ma.getAllocationSite()
            );
    }

    /** FIELD ACCESSES **/
    @Before(marker = BytecodeMarker.class, args = "putfield")
    public static void onFieldWrite(MyMethodStaticContext ma, DynamicContext dc) {
        SharingAnalysis.instanceOf().onFieldWrite(
                dc.getStackValue(1, Object.class), //accessed object
                ma.getFieldId()
            );
    }

    @Before(marker=BytecodeMarker.class, args = "getfield")
    public static void onFieldRead(MethodStaticContext sc, MyMethodStaticContext ma, DynamicContext dc) {
        SharingAnalysis.instanceOf().onFieldRead(
                dc.getStackValue(0, Object.class), //accessed object
                ma.getFieldId()
            );
    }
}
