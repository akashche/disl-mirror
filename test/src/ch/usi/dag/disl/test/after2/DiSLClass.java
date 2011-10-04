package ch.usi.dag.disl.test.after2;

import java.util.LinkedList;
import java.util.Stack;

import ch.usi.dag.disl.dislclass.annotation.AfterReturning;
import ch.usi.dag.disl.dislclass.annotation.AfterThrowing;
import ch.usi.dag.disl.dislclass.annotation.Before;
import ch.usi.dag.disl.dislclass.annotation.ThreadLocal;
import ch.usi.dag.disl.dislclass.snippet.marker.BodyMarker;
import ch.usi.dag.disl.staticinfo.analysis.uid.UniqueMethodId;
import ch.usi.dag.disl.test.after2.runtime.Analysis;

public class DiSLClass {
    
	@ThreadLocal
    static Stack<Integer> stackTL;
	
	@ThreadLocal
    static LinkedList<String> log;

    @Before(marker = BodyMarker.class, order = 0, scope = "*.*", guard = NotInitNorClinit.class)
    public static void onMethodEntryObjectArgs(UniqueMethodId id) {
        
    	Stack<Integer> thisStack = null;
        
        if((thisStack = stackTL) == null) {
            thisStack = (stackTL = new Stack<Integer>());
        }
        
        int idN = id.get();
        
        thisStack.push(idN);

        LinkedList<String> thisLog = null;
        
        if((thisLog = log) == null) {
            thisLog = (log = new LinkedList<String>());
        }
        
        Analysis.log(thisLog, "Up " + idN);
    }

    @AfterReturning(marker = BodyMarker.class, order = 0, scope = "*.*", guard = NotInitNorClinit.class)
    public static void onMethodRet(UniqueMethodId id) {
    	
        Analysis.onExit(stackTL, id.get(), log, "DownOk ");
    }
    
    @AfterThrowing(marker = BodyMarker.class, order = 0, scope = "*.*", guard = NotInitNorClinit.class)
    public static void onMethodExc(UniqueMethodId id) {
    	
    	Analysis.onExit(stackTL, id.get(), log, "DownEx ");
        
    }
}
