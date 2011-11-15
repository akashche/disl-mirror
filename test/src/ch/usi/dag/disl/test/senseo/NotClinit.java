package ch.usi.dag.disl.test.senseo;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.snippet.MarkedRegion;
import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.guard.SnippetGuard;

public class NotClinit implements SnippetGuard {
    @Override
    public boolean isApplicable(ClassNode classNode, MethodNode methodNode, Snippet snippet, MarkedRegion markedRegion) {
        return (methodNode.name.equals("<clinit>")) ? false : true;
    }
}
