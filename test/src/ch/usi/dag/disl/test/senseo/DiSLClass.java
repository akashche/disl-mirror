package ch.usi.dag.disl.test.senseo;

import ch.usi.dag.disl.dislclass.annotation.Before;
import ch.usi.dag.disl.dislclass.annotation.AfterReturning;
import ch.usi.dag.disl.dislclass.annotation.SyntheticLocal;
import ch.usi.dag.disl.dislclass.annotation.ThreadLocal;
import ch.usi.dag.disl.dislclass.snippet.marker.BodyMarker;
import ch.usi.dag.disl.guard.SnippetGuard;
import ch.usi.dag.disl.processor.Processor;
import ch.usi.dag.disl.processor.ProcessorApplyType;
import ch.usi.dag.disl.staticinfo.analysis.StaticContext;
import ch.usi.dag.disl.staticinfo.analysis.uid.UniqueMethodId;
import ch.usi.dag.disl.test.senseo.runtime.*;

// This is the SENSEO Case study recast in DiSL
// To run it you have to do once:
// ant extendThread  

// Then compile it:
// ant
// ant package -Dtest.name=senseo

// An run it:
// ./run-pkg.sh senseo

// To run dacapo:
// ./runDacapo.sh <bench>

// To enable full coverage, edit the conf/exclusion.lst

public class DiSLClass {
	@SyntheticLocal
	public static Analysis thisAnalysis;

	@ThreadLocal
	static Analysis currentAnalysis;

	// use the HasRefArgsGuard for the optimization
	@Before(marker = BodyMarker.class, order = 0, scope = "*.*" /*, guard = HasRefArgsGuard.class*/ )
	public static void onMethodEntry(StaticContext sc, UniqueMethodId id) {
	
		if((thisAnalysis = currentAnalysis) == null) {		
			thisAnalysis = currentAnalysis = new Analysis();
		}
		thisAnalysis.onMethodEntry(id.get());

		Processor.apply(ProcessorTest.class, ProcessorApplyType.INSIDE_METHOD);		
	}

	@AfterReturning(marker = BodyMarker.class, order = 0, scope = "*.*" /*, guard = HasRefArgsGuard.class */)
	public static void onMethodExit() {
		thisAnalysis.onMethodExit();
	}
}
