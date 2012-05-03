package ch.usi.dag.disl.example.hprof;

import org.objectweb.asm.tree.AbstractInsnNode;

import ch.usi.dag.disl.staticcontext.MethodStaticContext;

public class MyMethodStaticContext extends MethodStaticContext {
	public int getOffset() {
        int idx = 0;

        for(AbstractInsnNode instr = staticContextData.getRegionStart(); instr != null; instr = instr.getPrevious()) {
            idx++;
        }

        return idx;
	}

	public String getExtendedID() {
		return thisMethodFullName() + "_" + getOffset() + "_";		
	}

}
