package ch.usi.dag.disl.utilinstr.tlvinserter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

import ch.usi.dag.disl.localvar.ThreadLocalVar;


/**
 * Extends {@link Thread} with a "bypass" variable and writes its new bytecode
 * to a class file in a given directory. This is required to compile DiSL bypass
 * code, which checks the state of the "bypass" variable.
 */
public final class ExtendThread {

    public static void main (final String ... args) throws Exception {

        if (args.length < 1) {
            System.err.println ("usage: ExtendThread <output-directory>");
            System.exit (1);
        }

        final File outputDir  = new File (args [0]);
        if (!outputDir.isDirectory ()) {
            System.err.printf ("error: %s does not exist or is not a directory!\n", outputDir);
            System.exit (1);
        }

        //
        // Define a thread-local non-inheritable boolean variable named
        // "bypass", with a default value of false,
        //
        final ThreadLocalVar tlBypass = new ThreadLocalVar (
            null, "bypass", Type.getType (boolean.class), false
        );

        tlBypass.setDefaultValue (0);

        //
        // Extend Thread with a "bypass" variable and dump the new Thread
        // bytecode into given directory.
        //
        __writeThread (outputDir, __extendThread (tlBypass));
    }


    private static byte [] __extendThread (
        final ThreadLocalVar ... tlvs
    ) throws IOException {
        final InputStream is = Thread.class.getResourceAsStream ("Thread.class");
        final ClassReader cr = new ClassReader (is);
        final ClassWriter cw = new ClassWriter (cr, 0);
        final Set <ThreadLocalVar> vars = new HashSet <ThreadLocalVar> (Arrays.asList (tlvs));
        cr.accept (new TLVInserter (cw, vars), 0);
        return cw.toByteArray ();
    }


    private static void __writeThread (
        final File baseDir, final byte [] bytes
    ) throws IOException {
        final Class <Thread> tc = Thread.class;
        final String pkgName = tc.getPackage ().getName ();
        final String dirName = pkgName.replace ('.', File.separatorChar);

        final File outputDir = new File (baseDir, dirName);
        outputDir.mkdirs ();

        final String fileName = String.format ("%s.class", tc.getSimpleName ());
        final File outputFile = new File (outputDir, fileName);

        final FileOutputStream fos = new FileOutputStream (outputFile);
        try {
            fos.write (bytes);
        } finally {
            fos.close ();
        }
    }
}
