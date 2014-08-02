package ch.usi.dag.disl.classparser;

import java.lang.reflect.Field;
import java.util.Iterator;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.VarInsnNode;

import ch.usi.dag.disl.exception.DiSLFatalException;
import ch.usi.dag.disl.exception.ParserException;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.util.AsmHelper.Insns;
import ch.usi.dag.disl.util.ReflectionHelper;


// package visible
abstract class ParserHelper {

    public static Class <?> getGuard (final Type guardType)
    throws ReflectionException {

        if (guardType == null) {
            return null;
        }

        return ReflectionHelper.resolveClass (guardType);
    }


    // NOTE: first parameter is modified by this function
    public static <T> void parseAnnotation (
        final T parsedDataObject, final AnnotationNode annotation
    ) {
        try {

            // nothing to do
            if (annotation.values == null) {
                return;
            }

            final Iterator <?> it = annotation.values.iterator ();

            while (it.hasNext ()) {

                // get attribute name
                final String name = (String) it.next ();

                // find correct field
                final Field attr = parsedDataObject.getClass ().getField (name);

                if (attr == null) {
                    throw new DiSLFatalException ("Unknown attribute "
                        + name
                        + " in annotation "
                        + Type.getType (annotation.desc).toString ()
                        + ". This may happen if annotation class is changed"
                        + "  but parser class is not.");
                }

                // set attribute value into the field
                attr.set (parsedDataObject, it.next ());
            }

        } catch (final Exception e) {
            throw new DiSLFatalException (
                "Reflection error while parsing annotation", e);
        }
    }


    public static void usesContextProperly (
        final String className, final String methodName,
        final String methodDescriptor, final InsnList instructions
    ) throws ParserException {

        final Type [] types = Type.getArgumentTypes (methodDescriptor);
        int maxArgIndex = 0;

        // count the max index of arguments
        for (final Type type : types) {

            // add number of occupied slots
            maxArgIndex += type.getSize ();
        }

        // The following code assumes that all disl snippets are static
        for (final AbstractInsnNode instr : Insns.selectAll (instructions)) {

            switch (instr.getOpcode ()) {
            // test if the context is stored somewhere else
            case Opcodes.ALOAD: {

                final int local = ((VarInsnNode) instr).var;

                if (local < maxArgIndex
                    && instr.getNext ().getOpcode () == Opcodes.ASTORE) {
                    throw new ParserException ("In method " + className
                        + "." + methodName + " - method parameter"
                        + " (context) cannot be stored into local"
                        + " variable");
                }

                break;
            }
            // test if something is stored in the context
            case Opcodes.ASTORE: {

                final int local = ((VarInsnNode) instr).var;

                if (local < maxArgIndex) {
                    throw new ParserException ("In method " + className
                        + "." + methodName + " - method parameter"
                        + " (context) cannot be overwritten");
                }

                break;
            }
            }
        }
    }
}
