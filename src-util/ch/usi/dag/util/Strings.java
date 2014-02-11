package ch.usi.dag.util;

/**
 * Utility class providing miscellaneous string operations.
 *
 * @author Lubomir Bulej
 */
public final class Strings {

    /**
     * Empty string.
     */
    public static final String EMPTY_STRING = "";


    /**
     * Empty string array.
     */
    public static final String [] EMPTY_ARRAY = new String [0];

    //

    private Strings () {
        // pure static class - not to be instantiated
    }

    //

    /**
     * Joins multiple strings using a "glue" string.
     */
    public static String join (final String glue, final String ... fragments) {
        Assert.objectNotNull (glue, "glue");
        Assert.objectNotNull (fragments, "fragments");

        //

        final int fragmentCount = fragments.length;
        if (fragmentCount >= 2) {
            return __joinTwoOrMore (glue, fragmentCount, fragments);

        } else if (fragmentCount == 1) {
            return fragments [0];

        } else {
            return EMPTY_STRING;
        }
    }


    /**
     * Joins string representations of multiple objects using a "glue" string.
     */
    public static String join (final String glue, final Object ... fragments) {
        Assert.objectNotNull (glue, "glue");
        Assert.objectNotNull (fragments, "fragments");

        //

        final int fragmentCount = fragments.length;
        if (fragmentCount >= 2) {
            //
            // Convert the fragments to strings and the join their string
            // representations.
            //
            final String [] fragmentStrings = new String [fragmentCount];
            for (int i = 0; i < fragmentCount; i++) {
                fragmentStrings [i] = String.valueOf (fragments [i]);
            }

            return __joinTwoOrMore (glue, fragmentCount, fragmentStrings);

        } else if (fragmentCount == 1) {
            return String.valueOf (fragments [0]);

        } else {
            return EMPTY_STRING;
        }
    }


    private static String __joinTwoOrMore (
        final String glue, final int fragmentCount, final String [] fragments
    ) {
        //
        // To avoid reallocations in the StringBuilder, estimate the length
        // of the result to dimension the StringBuilder accordingly.
        //
        int length = glue.length () * (fragmentCount - 1);
        for (final String fragment : fragments) {
            length += fragment.length ();
        }

        //
        // Join the fragments using the glue. Since there are at least two
        // fragments, we can append the first fragment unconditionally
        // outside the loop body.
        //
        final StringBuilder builder = new StringBuilder (length);

        builder.append (fragments [0]);
        for (int i = 1; i < fragmentCount; i++) {
            builder.append (glue);
            builder.append (fragments [i]);
        }

        //

        return builder.toString ();
    }

}
