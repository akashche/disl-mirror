package ch.usi.dag.disl.processor;

import java.lang.reflect.Method;
import java.util.Set;

import ch.usi.dag.disl.annotation.ArgumentProcessor;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticContextGenException;
import ch.usi.dag.disl.localvar.LocalVars;
import ch.usi.dag.disl.snippet.Snippet;


/**
 * Represents an {@link ArgumentProcessor} method, which is analogous to a
 * {@link Snippet} in a DiSL class.
 */
public class ArgProcessorMethod {

    private final String __originClassName;
    private final String __originMethodName;

    private final Set <ArgProcessorKind> __types;
    private final Method __guard;

    private final ProcUnprocessedCode __templateCode;
    private ProcCode __expandedCode;

    //

    public ArgProcessorMethod (
        final String originClassName, final String originMethodName,
        final Set <ArgProcessorKind> types, final Method guard,
        final ProcUnprocessedCode templateCode
    ) {
        __originClassName = originClassName;
        __originMethodName = originMethodName;

        __types = types;
        __guard = guard;

        __templateCode = templateCode;
    }

    //

    /**
     * @return the canonical name of the class in which the argument processor
     *         was defined.
     */
    public String getOriginClassName () {
        return __originClassName;
    }


    /**
     * @return the name of the method representing the argument processor.
     */
    public String getOriginMethodName () {
        return __originMethodName;
    }


    /**
     * @return a fully qualified method name representing the argument processor.
     */
    public String getOriginName () {
        return __originClassName +"."+ __originMethodName;
    }

    //

    public Set <ArgProcessorKind> getTypes () {
        return __types;
    }


    public Method getGuard () {
        return __guard;
    }


    public ProcCode getCode () {
        return __expandedCode;
    }

    //

    public void init (final LocalVars localVars)
    throws StaticContextGenException, ReflectionException {
        __expandedCode = __templateCode.process (localVars);
    }

}
