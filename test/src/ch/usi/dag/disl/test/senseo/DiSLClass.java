package ch.usi.dag.disl.test.senseo;

import ch.usi.dag.disl.dislclass.annotation.After;
import ch.usi.dag.disl.dislclass.annotation.Before;
import ch.usi.dag.disl.dislclass.annotation.SyntheticLocal;
import ch.usi.dag.disl.dislclass.annotation.ThreadLocal;
import ch.usi.dag.disl.dislclass.snippet.marker.AfterInitBodyMarker;
import ch.usi.dag.disl.dislclass.snippet.marker.BodyMarker;
import ch.usi.dag.disl.processor.Processor;
import ch.usi.dag.disl.processor.ProcessorApplyType;
import ch.usi.dag.disl.staticinfo.analysis.BasicBlockAnalysis;
import ch.usi.dag.disl.staticinfo.analysis.uid.UniqueMethodId;
import ch.usi.dag.disl.test.senseo.runtime.Analysis;

// This is the SENSEO Case study recast in DiSL
// To compile it you have to do once:
// cd test; ant  

// An to run it:
// ./run.sh senseo pkg

// To disable full coverage, edit the conf/exclusion.lst

public class DiSLClass {
    @SyntheticLocal
    public static Analysis thisAnalysis;

    @ThreadLocal
    static Analysis currentAnalysis;

    /**************************************************
     **                Constructors                  **
     **************************************************/
    @Before(marker = AfterInitBodyMarker.class, order = 1, scope = "*.*", guard = ConstructorHasOnlyPrimitiveArgs.class)
    public static void onConstructorEntryPrimitiveArgs(UniqueMethodId id, BasicBlockAnalysis bba) {
        if((thisAnalysis = currentAnalysis) == null) {
            thisAnalysis = (currentAnalysis = new Analysis());
        }
        //TODO: add method getTotBBs() to class BasicBlockAnalysis
        thisAnalysis.onEntry(id.get(), true, -1);//bba.getTotBBs());
    }

    @Before(marker = AfterInitBodyMarker.class, order = 1, scope = "*.*", guard = ConstructorHasObjectArgs.class)
    public static void onConstructorEntryObjectArgs(UniqueMethodId id, BasicBlockAnalysis bba) {
        if((thisAnalysis = currentAnalysis) == null) {
            thisAnalysis = (currentAnalysis = new Analysis());
        }
        //TODO: add method getTotBBs() to class BasicBlockAnalysis
        thisAnalysis.onEntry(id.get(), false, -1);//bba.getTotBBs());

        Processor.apply(ArgumentProcessor.class, ProcessorApplyType.INSIDE_METHOD);     
    }

    @After(marker = BodyMarker.class, order = 1, scope = "*.*", guard = OnlyInit.class)
    public static void onConstructorExit() {
        thisAnalysis.onExit();
    }

    /**************************************************
     **                   Methods                    **
     **************************************************/
    @Before(marker = BodyMarker.class, order = 1, scope = "*.*", guard = MethodHasOnlyPrimitiveArgs.class)
    public static void onMethodEntryPrimitiveArgs(UniqueMethodId id, BasicBlockAnalysis bba) {
        if((thisAnalysis = currentAnalysis) == null) {
            thisAnalysis = (currentAnalysis = new Analysis());
        }
        //TODO: add method getTotBBs() to class BasicBlockAnalysis
        thisAnalysis.onEntry(id.get(), true, -1);//bba.getTotBBs());
    }

    @Before(marker = BodyMarker.class, order = 1, scope = "*.*", guard = MethodHasObjectArgs.class)
    public static void onMethodEntryObjectArgs(UniqueMethodId id, BasicBlockAnalysis bba) {
        if((thisAnalysis = currentAnalysis) == null) {
            thisAnalysis = (currentAnalysis = new Analysis());
        }
        //TODO: add method getTotBBs() to class BasicBlockAnalysis
        thisAnalysis.onEntry(id.get(), false, -1);//bba.getTotBBs());

        Processor.apply(ArgumentProcessor.class, ProcessorApplyType.INSIDE_METHOD);     
    }

    @After(marker = BodyMarker.class, order = 1, scope = "*.*", guard = NotInitNorClinit.class)
    public static void onMethodExit() {
        thisAnalysis.onExit();
    }

    /**************************************************
     **              Allocated objects               **
     **************************************************/
//    @AfterReturning(marker = BytecodeMarker.class, param="invokespecial", scope = "TargetClass.*", guard = IsCallToConstructor.class)
//    public static void beforeNew(DynamicContext di) {
//        thisAnalysis.onAlloc(di.getStackValue(0, Object.class)); // perform analysis
//    }

    /**************************************************
     **                     BBs                      **
     **************************************************/
//    @Before(marker = BasicBlockMarker.class, scope = "*.*", order = 2, guard = NotClinit.class)
//    public static void bbAnalysis1(BasicBlockAnalysis bba) {
//        thisAnalysis.onBB(bba.getBBindex());
//    }
}
