package ch.usi.dag.disl.processor.generator;

import ch.usi.dag.disl.processor.ArgProcessorKind;
import ch.usi.dag.disl.processor.ProcCode;


public class ProcMethodInstance {

    private final int argPos;
    private final int argsCount;
    private final ArgProcessorKind argType;
    private final String argTypeDesc;
    private final ProcCode code;

    //

    public ProcMethodInstance (
        final int argPos, final int argsCount, final ArgProcessorKind argType,
        final String argTypeDesc, final ProcCode code
    ) {
        this.argPos = argPos;
        this.argsCount = argsCount;
        this.argType = argType;
        this.argTypeDesc = argTypeDesc;
        this.code = code;
    }


    public int getArgPos () {
        return argPos;
    }


    public int getArgsCount () {
        return argsCount;
    }


    public ArgProcessorKind getArgType () {
        return argType;
    }


    public String getArgTypeDesc () {
        return argTypeDesc;
    }


    // Note: Code is NOT cloned for each ProcMethodInstance.
    // If the weaver does not rely on this, we can reuse processor instances
    // which can save us some computation
    public ProcCode getCode () {
        return code;
    }

}
