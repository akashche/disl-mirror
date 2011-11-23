package ch.usi.dag.disl.example.fieldsImmutabilityAnalysis;


import ch.usi.dag.disl.annotation.GuardMethod;


public abstract class OnlyInit  {
  
	@GuardMethod
	public static boolean isApplicable(MyStaticContext sc) {
		return (sc.isInTheConstructor());
	}
}
