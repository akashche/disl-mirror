package ch.usi.dag.disl.example.classgen.shadowheap.instrument;

import org.objectweb.asm.tree.AbstractInsnNode;

import ch.usi.dag.disl.staticcontext.MethodStaticContext;

/**
 * @author Andreas Sewe
 */
public class AllocationSiteStaticContext extends MethodStaticContext {

	/**
	 * The character used to separate the components of the allocation site identifier.
	 */
	public static final char SUBSEP = '\034';

	public String getAllocationSite() {
		int index = 0;

		for (AbstractInsnNode instruction = staticContextData.getRegionStart(); instruction != null; instruction = instruction.getPrevious())
			index++;

		return thisMethodFullName() + SUBSEP + thisMethodDescriptor() + SUBSEP + index;
	}

	public String getAllocationSiteForConstructorNewInstance() {
		return "java/lang/reflect/Constructor.newInstance" + SUBSEP + "([Ljava/lang/Object;)Ljava/lang/Object;" + SUBSEP + -1;
	}

	public String getAllocationSiteForArrayNewInstance() {
		return "java/lang/reflect/Array.newInstance" + SUBSEP + "(Ljava/lang/Class;I)Ljava/lang/Object;" + SUBSEP + -1;
	}

	public String getAllocationSiteForMultiArrayNewInstance() {
		return "java/lang/reflect/Array.newInstance" + SUBSEP + "(Ljava/lang/Class;[I)Ljava/lang/Object;" + SUBSEP + -1;
	}
}
