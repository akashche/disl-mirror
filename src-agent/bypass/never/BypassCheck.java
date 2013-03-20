package ch.usi.dag.disl.dynamicbypass;

/**
 * Checks whether to bypass instrumented code. This version is to be used when
 * dynamic bypass is disabled. After the JVM bootstrap phase, this will completely
 * disable execution of uninstrumented code.
 */
public final class BypassCheck {

    public static boolean executeUninstrumented () {
        return false;
    }

}
