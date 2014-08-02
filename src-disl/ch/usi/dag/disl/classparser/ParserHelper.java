package ch.usi.dag.disl.classparser;

import java.lang.reflect.Field;
import java.util.Iterator;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import ch.usi.dag.disl.exception.DiSLFatalException;
import ch.usi.dag.disl.exception.ParserException;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.util.AsmHelper;
import ch.usi.dag.disl.util.AsmHelper.Insns;
import ch.usi.dag.disl.util.Insn;
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
                    throw new DiSLFatalException (
                        "Unknown attribute "+ name +" in annotation "+
                        Type.getType (annotation.desc).toString () +
                        ". This may happen if annotation class is changed"+
                        "  but parser class is not."
                    );
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
        final String className, final MethodNode method
    ) throws ParserException {
        //
        // Check accesses to method parameters to ensure that the context
        // is not overwritten or stored to a local variable.
        //
        // WARNING: The following code assumes that DiSL snippets are
        // static methods, and therefore do not have the "this" parameter.
        //
        final int firstLocalSlot = AsmHelper.getParameterSlotCount (method);
        for (final AbstractInsnNode insn : Insns.selectAll (method.instructions)) {
            if (! (insn instanceof VarInsnNode)) {
                continue;
            }

            final int localSlot = ((VarInsnNode) insn).var;
            if (Insn.ASTORE.matches (insn)) {
                //
                // Check for stores into context parameter slot.
                //
                if (localSlot < firstLocalSlot) {
                    throw new ParserException (
                        "In method " + className
                        + "." + method.name + " - method parameter"
                        + " (context) cannot be overwritten"
                    );
                }

            } else if (Insn.ALOAD.matches (insn)) {
                //
                // Check that an instruction following a context load is not a
                // store to a local variable. This is just a sanity check -- we
                // would need escape analysis to handle this properly (i.e. to
                // avoid passing context to some method).
                //
                if (localSlot < firstLocalSlot) {
                    if (Insn.ASTORE.matches (Insns.FORWARD.nextRealInsn (insn))) {
                        throw new ParserException (
                            "In method " + className
                            + "." + method.name + " - method parameter"
                            + " (context) cannot be stored into local"
                            + " variable"
                        );
                    }
                }
            }
        } // for

    }

}

