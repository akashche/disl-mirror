package ch.usi.dag.disl.test.utils;

import java.util.concurrent.TimeUnit;


public final class Duration {

    private final long __value;
    private final TimeUnit __unit;

    //

    private Duration (final long value, final TimeUnit unit) {
        __value = value;
        __unit = unit;
    }

    //

    public final long value () {
        return __value;
    }

    public final TimeUnit unit () {
        return __unit;
    }

    public final long to (final TimeUnit targetUnit) {
        return targetUnit.convert (__value, __unit);
    }

    //

    public final void timedWait (final Object object) throws InterruptedException {
        __unit.timedWait (object, __value);
    }

    public final void timedJoin (final Thread thread) throws InterruptedException {
        __unit.timedJoin (thread, __value);
    }

    //

    public final void sleep () throws InterruptedException {
        __unit.sleep (__value);
    }

    public final void softSleep () {
        try {
            __unit.sleep (__value);

        } catch (final InterruptedException ie) {
            // return if interrupted, keeping the interrupted() status
        }
    }

    //

    public static Duration of (final long duration, final TimeUnit timeUnit) {
        return new Duration (duration, timeUnit);
    }

}
