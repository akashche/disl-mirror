package ch.usi.dag.disl.annotation;

public @interface SyntheticStaticField {

	public enum Scope {
		PERCLASS, PERMETHOD
	}

	Scope scope() default (Scope.PERMETHOD);
}
