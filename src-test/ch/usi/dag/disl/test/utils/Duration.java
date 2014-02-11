package ch.usi.dag.disl.test.utils;

import java.util.concurrent.TimeUnit;


/**
 *
 * @author Lubomir Bulej
 */
public final class Duration {

    private final long __amount;
    private final TimeUnit __unit;

    //

    private Duration (final long amount, final TimeUnit unit) {
        __amount = amount;
        __unit = unit;
    }

    //

    /**
     * Converts this {@link Duration} to given time units.
     *
     * @param unit
     *        the time unit to convert this {@link Duration} to.
     * @return the amount of the given time units representing this
     *         {@link Duration}.
     */
    public long to (final TimeUnit unit) {
        return unit.convert (__amount, __unit);
    }

    //

    /**
     * @see TimeUnit#timedWait(Object, long)
     */
    public void timedWait (final Object object) throws InterruptedException {
        __unit.timedWait (object, __amount);
    }

    /**
     * @see TimeUnit#timedJoin(Thread, long)
     */
    public void timedJoin (final Thread thread) throws InterruptedException {
        __unit.timedJoin (thread, __amount);
    }

    //

    /**
     * @see TimeUnit#sleep(long)
     */
    public void sleep () throws InterruptedException {
        __unit.sleep (__amount);
    }

    public void softSleep () {
        try {
            __unit.sleep (__amount);

        } catch (final InterruptedException ie) {
            // return if interrupted, keeping the interrupted() status
        }
    }

    //

    /**
     * Creates a {@link Duration} instance representing a given amount of given
     * time units.
     *
     * @param amount
     *      the amount of time units
     * @param unit
     *      the time unit representing the granularity of the duration
     * @return
     */
    public static Duration of (final long amount, final TimeUnit unit) {
        return new Duration (amount, unit);
    }

}
