package ch.usi.dag.disl.example.senseo;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.AfterReturning;
import ch.usi.dag.disl.annotation.AfterThrowing;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.annotation.SyntheticLocal;
import ch.usi.dag.disl.annotation.ThreadLocal;
import ch.usi.dag.disl.dynamiccontext.DynamicContext;
import ch.usi.dag.disl.example.senseo.runtime.Analysis;
import ch.usi.dag.disl.marker.BodyMarker;
import ch.usi.dag.disl.marker.BytecodeMarker;
import ch.usi.dag.disl.processorcontext.ArgumentProcessorContext;
import ch.usi.dag.disl.processorcontext.ArgumentProcessorMode;
import ch.usi.dag.disl.staticcontext.uid.UniqueMethodId;

public class DiSLClass {
    @SyntheticLocal
    public static Analysis thisAnalysis;

    @ThreadLocal
    static Analysis currentAnalysis;

    /**************************************************
     **                Constructors                  **
     **************************************************/
    @Before(marker = BodyMarker.class, order = 0, guard = ConstructorHasOnlyPrimitiveArgs.class, dynamicBypass = false)
    public static void onConstructorEntryPrimitiveArgs(UniqueMethodId id) {
        if((thisAnalysis = currentAnalysis) == null) {
            thisAnalysis = (currentAnalysis = new Analysis());
        }
        thisAnalysis.onEntry(id.get(), true);
    }

    @Before(marker = BodyMarker.class, order = 0, guard = ConstructorHasObjectArgs.class, dynamicBypass = false)
    public static void onConstructorEntryObjectArgs(UniqueMethodId id, ArgumentProcessorContext pc) {
        if((thisAnalysis = currentAnalysis) == null) {
            thisAnalysis = (currentAnalysis = new Analysis());
        }
        thisAnalysis.onEntry(id.get(), false);

        pc.apply(MyArgumentProcessor.class, ArgumentProcessorMode.METHOD_ARGS);     
    }

    @After(marker = BodyMarker.class, order = 0, guard = OnlyInit.class, dynamicBypass = false)
    public static void onConstructorExit() {
        thisAnalysis.onExit();
    }

    /**************************************************
     **                   Methods                    **
     **************************************************/
    @Before(marker = BodyMarker.class, order = 0, guard = MethodHasOnlyPrimitiveArgs.class, dynamicBypass = false)
    public static void onMethodEntryPrimitiveArgs(UniqueMethodId id) {
        if((thisAnalysis = currentAnalysis) == null) {
            thisAnalysis = (currentAnalysis = new Analysis());
        }
        thisAnalysis.onEntry(id.get(), true);
    }

    @Before(marker = BodyMarker.class, order = 0, guard = MethodHasObjectArgs.class, dynamicBypass = false)
    public static void onMethodEntryObjectArgs(UniqueMethodId id, ArgumentProcessorContext pc) {
        if((thisAnalysis = currentAnalysis) == null) {
            thisAnalysis = (currentAnalysis = new Analysis());
        }
        thisAnalysis.onEntry(id.get(), false);

        pc.apply(MyArgumentProcessor.class, ArgumentProcessorMode.METHOD_ARGS);    
    }

    @AfterReturning(marker = BodyMarker.class, order = 0, guard = MethodReturnsObj.class, dynamicBypass = false)
    public static void onMethodReturnObj(DynamicContext dc) {
        Object obj = dc.getStackValue(0, Object.class);
        thisAnalysis.onExit(obj);
    }

    @AfterReturning(marker = BodyMarker.class, order = 0, guard = MethodReturnsSomethingElse.class, dynamicBypass = false)
    public static void onMethodReturnsSomethingElse() {
        thisAnalysis.onExit();
    }

    @AfterThrowing(marker = BodyMarker.class, order = 1, guard = NotInitNorClinit.class, dynamicBypass = false)
    public static void onMethodThrow() {
        thisAnalysis.onExit();
    }

    /**************************************************
     **            Calls to constructors             **
     **************************************************/
    @AfterReturning(marker = BytecodeMarker.class, args = "new, newarray, anewarray, multianewarray", order = 0, guard = NotClinit.class, dynamicBypass = false)
    public static void beforeNew() {
        thisAnalysis.onAlloc();
    }
}
