package ch.usi.dag.disl.util;


/**
 * Utility class providing methods for working with Java-specific class and
 * methods names.
 * <p>
 * <b>Note:</b> This class is not part of the DiSL API.
 *
 * @author Lubomir Bulej
 */
public final class JavaNames {

    private JavaNames () {
        // TODO Auto-generated constructor stub
    }

    //

    private static final String __CONSTRUCTOR_NAME__ = "<init>";
    private static final String __INITIALIZER_NAME__ = "<clinit>";

    public static final boolean isConstructorName (final String name) {
        return __CONSTRUCTOR_NAME__.equals (name);
    }

    public static final boolean isInitializerName (final String name) {
        return __INITIALIZER_NAME__.equals (name);
    }

    //

    private static final char __CANONICAL_PKG_SEPARATOR_CHAR__ = '.';
    private static final char __INTERNAL_PKG_SEPARATOR_CHAR__ = '/';


    /**
     * @return Canonical class name for the given internal class name.
     */
    public static String internalToCanonical (final String name) {
        return name.replace (__INTERNAL_PKG_SEPARATOR_CHAR__, __CANONICAL_PKG_SEPARATOR_CHAR__);
    }

    public static String canonicalToInternal (final String name) {
        return name.replace (__CANONICAL_PKG_SEPARATOR_CHAR__, __INTERNAL_PKG_SEPARATOR_CHAR__);
    }

    //

    private static final String __CLASS_FILE_EXTENSION__ = ".class";

    public static String appendClassFileExtension (final String name) {
        return name + __CLASS_FILE_EXTENSION__;
    }

    public static String stripClassFileExtension (final String name) {
        final int extensionStart = name.lastIndexOf (__CLASS_FILE_EXTENSION__);
        return (extensionStart >= 0) ? name.substring (0, extensionStart) : name;
    }

    public static boolean hasClassFileExtension (final String name) {
        return name.endsWith (__CLASS_FILE_EXTENSION__);
    }

    //

    public static boolean hasInternalPackageName (final String name) {
        return name.indexOf (__INTERNAL_PKG_SEPARATOR_CHAR__, 0) >= 0;
    }

    public static boolean hasCanonicalPackageName (final String name) {
        return name.indexOf (__CANONICAL_PKG_SEPARATOR_CHAR__, 0) >= 0;
    }

    private static final String __INTERNAL_PKG_SEPARATOR__ =
        String.valueOf (__INTERNAL_PKG_SEPARATOR_CHAR__);

    public static String joinInternal (final String ... elements) {
        return String.join (__INTERNAL_PKG_SEPARATOR__, elements);
    }


    private static final String __CANONICAL_PKG_SEPARATOR__ =
        String.valueOf (__CANONICAL_PKG_SEPARATOR_CHAR__);

    public static String joinCanonical (final String ... elements) {
        return String.join (__CANONICAL_PKG_SEPARATOR__, elements);
    }

    //

    public static String simpleClassName (final Object object) {
        final String className = object.getClass ().getName ();
        return className.substring (className.lastIndexOf (".") + 1);
    }

}
