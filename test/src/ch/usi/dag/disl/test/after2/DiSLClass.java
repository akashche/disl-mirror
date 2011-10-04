package ch.usi.dag.disl.test.after2;

import java.util.Stack;

import ch.usi.dag.disl.dislclass.annotation.After;
import ch.usi.dag.disl.dislclass.annotation.Before;
import ch.usi.dag.disl.dislclass.annotation.ThreadLocal;
import ch.usi.dag.disl.dislclass.snippet.marker.BodyMarker;
import ch.usi.dag.disl.staticinfo.analysis.uid.UniqueMethodId;
import ch.usi.dag.disl.test.after2.runtime.Analysis;

public class DiSLClass {
    @ThreadLocal
    static Stack<Integer> stackTL;

    @Before(marker = BodyMarker.class, order = 0, scope = "*.*", guard = NotInitNorClinit.class)
    public static void onMethodEntryObjectArgs(UniqueMethodId id) {
        Stack<Integer> thisStack;
        if((thisStack = stackTL) == null) {
            thisStack = (stackTL = new Stack<Integer>());
        }
        thisStack.push(id.get());
    }

    @After(marker = BodyMarker.class, order = 0, scope = "*.*", guard = NotInitNorClinit.class)
    public static void onMethodExit(UniqueMethodId id) {
        Analysis.onExit(stackTL, id.get());
    }
}
