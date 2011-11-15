package ch.usi.dag.disl.test.senseo;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.marker.MarkedRegion;

// check if none of the arguments is an Object 
// --> if "L" is not in the description of the arguments
public class ConstructorHasOnlyPrimitiveArgs extends OnlyInit {
    @Override
    public boolean isApplicable(ClassNode classNode, MethodNode methodNode, Snippet snippet, MarkedRegion markedRegion) {
        String desc = methodNode.desc;
        return
            super.isApplicable(classNode, methodNode, snippet, markedRegion)
            && !desc.substring(0, desc.indexOf(')')).contains("L");
    }
}
