package ch.usi.dag.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;


/**
 * Utility class providing miscellaneous file- and path-related operations.
 *
 * @author Lubomir Bulej
 */
public final class Files {

    private Files () {
        // pure static class - not to be instantiated
    }

    //

    /**
     * Stores a string into a file.
     *
     * @param fileName
     *        the name of the file to write.
     * @param content
     *        the string to write.
     * @throws FileNotFoundException
     *         if the given file could not be created
     * @throws IOException
     *         if an error occurs while writing to the file
     */
    public static void storeString (
        final String fileName, final String content
    ) throws FileNotFoundException {
        __storeStringAndClose (new PrintWriter (fileName), content);
    }


    /**
     * Stores a string into a file.
     *
     * @param fileName
     *        the name of the file to write.
     * @param content
     *        the string to write.
     * @throws FileNotFoundException
     *         if the given file could not be created
     * @throws IOException
     *         if an error occurs while writing to the file
     */
    public static void storeString (
        final File fileName, final String content
    ) throws FileNotFoundException {
        __storeStringAndClose (new PrintWriter (fileName), content);
    }

    private static void __storeStringAndClose (
        final PrintWriter output, final String content
    ) {
        try {
            output.print (content);

        } finally {
            output.close ();
        }
    }

    //

    /**
     * Loads an entire file into a string. This method should be only used for
     * reasonable sized text files.
     *
     * @param fileName
     *        the name of the file to read
     * @return a {@link String} with the contents of the given file
     * @throws FileNotFoundException
     *         if the given file could not be found
     * @throws IOException
     *         if the file could not be read
     */
    public static String loadString (final String fileName) throws IOException {
        return __drainToStringAndClose (new FileInputStream (fileName));
    }


    /**
     * Loads an entire file into a string. This method should be only used for
     * reasonably sized text files.
     *
     * @param fileName
     *        the name of the file to read
     * @return a {@link String} with the contents of the given file
     * @throws FileNotFoundException
     *         if the given file could not be found
     * @throws IOException
     *         if the file could not be read
     */
    public static String loadString (final File fileName) throws IOException {
        return __drainToStringAndClose (new FileInputStream (fileName));
    }


    /**
     * Loads an entire resource associated with a given class into a string.
     * This method should be only used for reasonably sized text resources.
     *
     * @param refClass
     *        the reference class to use when looking for an associated resource
     * @param name
     *        the name of the resource to read
     * @return a {@link String} with the contents of the given resource, or
     *         {@code null} if the resource could not be found
     * @throws FileNotFoundException
     *         if the given resource could not be found
     * @throws IOException
     *         if the resource could not be read
     */
    public static String loadStringResource (
        final Class <?> refClass, final String name
    ) throws IOException {
        final InputStream input = refClass.getResourceAsStream (name);
        if (input != null) {
            return __drainToStringAndClose (input);

        } else {
            throw new FileNotFoundException ("no such resource: "+ name);
        }
    }


    private static String __drainToStringAndClose (
        final InputStream input
    ) throws IOException {
        try {
            return __drainToString (input);

        } finally {
            input.close ();
        }
    }


    private static String __drainToString (
        final InputStream input
    ) throws IOException {
        final ByteArrayOutputStream output = new ByteArrayOutputStream ();

        final int bufferSize = 4096;
        final byte [] buffer = new byte [bufferSize];

        READ_LOOP: while (true) {
            final int bytesRead = input.read (buffer, 0, bufferSize);
            if (bytesRead < 1) {
                break READ_LOOP;
            }

            output.write (buffer, 0, bytesRead);
        }

        return output.toString ();
    }
}
