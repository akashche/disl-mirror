package ch.usi.dag.disl.classparser;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import ch.usi.dag.disl.annotation.ArgumentProcessor;
import ch.usi.dag.disl.exception.GuardException;
import ch.usi.dag.disl.exception.MarkerException;
import ch.usi.dag.disl.exception.ParserException;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticContextGenException;
import ch.usi.dag.disl.localvar.LocalVars;
import ch.usi.dag.disl.processor.ArgProcessor;
import ch.usi.dag.disl.snippet.Snippet;


/**
 * Parser for DiSL classes containing either snippets or method argument
 * processors.
 */
public class DislClassParser {

    private final SnippetParser __snippetParser = new SnippetParser ();

    private final ArgProcessorParser __argProcParser = new ArgProcessorParser ();

    //

    public void parse (final InputStream classBytes)
    throws ParserException, ReflectionException, StaticContextGenException,
        MarkerException, GuardException {

        //
        // Get an ASM representation of the DiSL class first, then parse it
        // as a snippet or an argument processor depending on the annotations
        // associated with the class.
        //
        final ClassNode classNode = __createClassNode (classBytes);
        if (__isArgumentProcessor (classNode)) {
            __argProcParser.parse (classNode);
        } else {
            __snippetParser.parse (classNode);
        }
    }


    private boolean __isArgumentProcessor (final ClassNode classNode) {
        //
        // An argument processor must have an @ArgumentProcessor annotation
        // associated with the class. DiSL instrumentation classes may have
        // an @Instrumentation annotation. DiSL classes without annotations
        // are by default considered to be instrumentation classes.
        //
        if (classNode.invisibleAnnotations != null) {
            final Type apType = Type.getType (ArgumentProcessor.class);

            for (final AnnotationNode annotation : classNode.invisibleAnnotations) {
                final Type annotationType = Type.getType (annotation.desc);
                if (apType.equals (annotationType)) {
                    return true;
                }
            }
        }

        // default: not an argument processor
        return false;
    }


    private ClassNode __createClassNode (final InputStream is)
    throws ParserException {
        //
        // Parse input stream into a class node. Include debug information so
        // that we can report line numbers in case of problems in DiSL classes.
        // Re-throw any exceptions as DiSL exceptions.
        //
        try {
            final ClassReader classReader = new ClassReader (is);
            final ClassNode classNode = new ClassNode ();
            classReader.accept (classNode, ClassReader.SKIP_FRAMES);
            return classNode;

        } catch (final IOException ioe) {
            throw new ParserException (ioe);
        }
    }

    //

    public LocalVars getAllLocalVars () {
        //
        // Merge all local variables from snippets and argument processors.
        //
        final LocalVars result = new LocalVars ();
        result.putAll (__snippetParser.getAllLocalVars ());
        result.putAll (__argProcParser.getAllLocalVars ());

        return result;
    }


    public List <Snippet> getSnippets () {
        return __snippetParser.getSnippets ();
    }


    public Map <Type, ArgProcessor> getProcessors () {
        return __argProcParser.getProcessors ();
    }

}
