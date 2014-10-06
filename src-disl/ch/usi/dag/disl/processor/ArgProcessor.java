package ch.usi.dag.disl.processor;

import java.util.List;

import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticContextGenException;
import ch.usi.dag.disl.localvar.LocalVars;


/**
 * Argument processor is a collection of argument processor methods. Each method
 * is similar to a snippet and can be specialized for a particular method
 * parameter or a type of method parameters.
 */
public class ArgProcessor {

    /**
     * Canonical name of the argument processor class.
     */
    private final String __className;

    private final List <ArgProcessorMethod> __methods;

    //

    public ArgProcessor (final String name, final List <ArgProcessorMethod> methods) {
        __className = name;
        __methods = methods;
    }


    /**
     * @return canonical name of the argument processor class.
     */
    public String getName () {
        return __className;
    }


    public List <ArgProcessorMethod> getMethods () {
        return __methods;
    }


    public void init (final LocalVars localVars)
    throws StaticContextGenException, ReflectionException {
        for (final ArgProcessorMethod method : __methods) {
            method.init (localVars);
        }
    }

}
