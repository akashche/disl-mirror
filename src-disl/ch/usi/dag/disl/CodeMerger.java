package ch.usi.dag.disl;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.CodeSizeEvaluator;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import ch.usi.dag.disl.dynamicbypass.BypassCheck;
import ch.usi.dag.disl.exception.DiSLFatalException;
import ch.usi.dag.disl.util.AsmHelper;


abstract class CodeMerger {

    private static final String BPC_CLASS = Type.getInternalName (BypassCheck.class);

    private static final String BPC_METHOD = "executeUninstrumented";

    private static final String BPC_DESC = "()Z";

    private static final int ALLOWED_SIZE = 64 * 1024; // 64KB limit


    // NOTE: the instCN ClassNode will be modified in the process
    // NOTE: abstract/native methods should not be included in changedMethods list
    public static ClassNode mergeClasses (
        final ClassNode origCN, final ClassNode instCN,
        final Set <String> changedMethods
    ) {
        // NOTE: that instrumentedCN can contain added fields
        // - has to be returned
        if (changedMethods == null) {
            throw new DiSLFatalException (
                "Set of changed methods cannot be null");
        }

        //
        // Selected instrumented methods and merge into their code the original
        // (un-instrumented) method to be executed when the bypass is active.
        // Duplicate the original method code to preserve it for the case
        // where the resulting method is too long.
        //
        instCN.methods.parallelStream ()
            .filter (instMN -> changedMethods.contains (instMN.name + instMN.desc))
            .forEach (instMN -> {
                final MethodNode cloneMN = AsmHelper.cloneMethod (
                    __findMethodNode (origCN, instMN.name, instMN.desc)
                );

                __createBypassCheck (
                    instMN.instructions, instMN.tryCatchBlocks,
                    cloneMN.instructions, cloneMN.tryCatchBlocks
                );
            });

        return instCN;
    }


    private static void __createBypassCheck (
        final InsnList instCode, final List <TryCatchBlockNode> instTcbs,
        final InsnList origCode, final List <TryCatchBlockNode> origTcbs
    ) {
        // The bypass check code has the following layout:
        //
        //     if (!BypassCheck.executeUninstrumented ()) {
        //         <instrumented code>
        //     } else {
        //         <original code>
        //     }
        //
        final MethodInsnNode checkNode = new MethodInsnNode (
            Opcodes.INVOKESTATIC, BPC_CLASS, BPC_METHOD, BPC_DESC, false
        );
        instCode.insert (checkNode);

        final LabelNode origLabel = new LabelNode ();
        instCode.insert (checkNode, new JumpInsnNode (Opcodes.IFNE, origLabel));

        instCode.add (origLabel);
        instCode.add (origCode);
        instTcbs.addAll (origTcbs);
    }


    // NOTE: the originalCN and instrumentedCN will be destroyed in the process
    // NOTE: abstract or native methods should not be included in the
    // changedMethods list
    public static ClassNode fixupMethods (
        final ClassNode origCN, final ClassNode instCN,
        final boolean splitLongMethods
    ) {
        //
        // Choose fixup strategy so that we don't have to it when processing.
        //
        final Consumer <FixupState> fixupStrategy = splitLongMethods ?
            fs -> fs.splitLongMethod () : fs -> fs.revertToOriginal ();

        IntStream.range (0, instCN.methods.size ())
            .mapToObj (i -> new FixupState (instCN, i))
            .collect (Collectors.toList ()).parallelStream ().unordered ()
            .filter (s -> s.instMethodSize () > ALLOWED_SIZE)
            .forEach (s -> {
                s.attachOrigMethod (origCN);
                fixupStrategy.accept (s);
            });

        return instCN;
    }


    private static final class FixupState {
        final ClassNode instClass;
        final int instIndex;

        // The following fields are assigned during processing.

        int instSize = -1;
        MethodNode origMethod;
        ClassNode origClass;

        //

        FixupState (final ClassNode instClass, final int instIndex) {
            this.instClass = instClass;
            this.instIndex = instIndex;
        }


        int instMethodSize () {
            if (instSize == -1) {
                final CodeSizeEvaluator cse = new CodeSizeEvaluator (null);
                instMethod ().accept (cse);
                instSize = cse.getMaxSize ();
            }

            return instSize;
        }


        MethodNode instMethod () {
            return instClass.methods.get (instIndex);
        }


        void attachOrigMethod (final ClassNode origClass) {
            final MethodNode im = instMethod ();
            origMethod = __findMethodNode (origClass, im.name, im.desc);
            this.origClass = origClass;
        }


        void splitLongMethod () {
            // TODO jb ! add splitting for to long methods
            // - ignore clinit - output warning
            // - output warning if splitted is to large and ignore

            // check the code size of the instrumented method
            // add if to the original method that jumps to the renamed instrumented
            // method
            // add original method to the instrumented code
            // rename instrumented method
        }


        void revertToOriginal () {
            //
            // Replace the instrumented method with the original method,
            // and print a warning about it.
            //
            instClass.methods.set (instIndex, origMethod);

            System.err.printf (
                "warning: method %s.%s not instrumented, because its size "+
                "(%d) exceeds the maximal allowed method size (%d)\n",
                instMethod ().name, instMethod ().desc, instMethodSize (),
                ALLOWED_SIZE
            );
        }
    }


    private static MethodNode __findMethodNode (
        final ClassNode cn, final String name, final String desc
    ) {
        return cn.methods.parallelStream ().unordered ()
            .filter (m -> m.name.equals (name) && m.desc.equals (desc))
            .findAny ().orElseThrow (() -> new RuntimeException (
                "Code merger fatal error: method for merge not found"
            ));
    }

}
