package ch.usi.dag.disl.example.fieldsImmutabilityAnalysis;

import ch.usi.dag.disl.annotation.GuardMethod;
import ch.usi.dag.disl.example.fieldsImmutabilityAnalysis.MyStaticContext;
public abstract class NoClInit {
  
	@GuardMethod
	public static boolean isApplicable(MyStaticContext sc) {
		return (!sc.isInTheStaticInitializer());
	}
}
