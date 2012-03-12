package ch.usi.dag.dislreserver.reflectiveinfo;

import org.objectweb.asm.tree.MethodNode;

public class MethodInfo {
    public MethodInfo(MethodNode methodNode) {
        //TODO: add the code to:
        //*) parse methodNode
        //*) initialize all fields required for the following methods
    }

    public String getName() { return null; }
    public int getModifiers() { return -1; }
    public String getReturnType() { return null; }
    public String[] getParameterTypes() { return null; }
    public String[] getExceptionTypes() { return null; }
}
