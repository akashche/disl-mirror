package ch.usi.dag.disl.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


// TODO add tool that will look for Instrumentation annotation and create meta-data for jar
// http://stackoverflow.com/questions/3644069/java-6-annotation-processing-configuration-with-ant
/**
 * Marks a class as a DiSL instrumentation class.
 * <p>
 * This annotation should be used with classes.
 */
@Documented
@Target (ElementType.TYPE)
@Retention (RetentionPolicy.CLASS)
public @interface Instrumentation {

}
