package ch.usi.dag.disl.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks guard validation method.
 * 
 * Guard method should be static and state-less.
 * 
 * Method argument can be Shadow, StaticContext and for ProcessorContext method guard
 * also ArgumentContext.
 */
@Retention(RetentionPolicy.RUNTIME) // to resolve annotation using reflection 
public @interface GuardMethod {

}
