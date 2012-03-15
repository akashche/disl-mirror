package ch.usi.dag.disl.example.classgen.shadowheap.instrument;

import ch.usi.dag.disl.annotation.AfterReturning;
import ch.usi.dag.disl.dynamiccontext.DynamicContext;
import ch.usi.dag.disl.example.classgen.shadowheap.runtime.AbstractShadowHeapAnalysis;
import ch.usi.dag.disl.marker.BodyMarker;
import ch.usi.dag.disl.marker.BytecodeMarker;

/**
 * This DiSL class instruments all object allocations.
 * 
 * @author Andreas Sewe
 */
public class AbstractShadowHeapInstrumentation {

	/**
	 * Instruments the allocation of a single object (<code>new</code>) or array (<code>newarray</code>,
	 * <code>anewarray</code).
	 * 
	 * Note that this snippet requires that bytecode verification is switched off (<code>-noverify</code>) as the newly
	 * created instances are passed to the runtime <em>prior</em> to initialization.
	 */
	@AfterReturning(marker = BytecodeMarker.class, args = "new,newarray,anewarray", dynamicBypass = true)
	public static void objectAllocated(DynamicContext dc, AllocationSiteStaticContext sc) {
		AbstractShadowHeapAnalysis.getInstance().onObjectAllocation(dc.getStackValue(0, Object.class), sc.getAllocationSite());
	}

	/**
	 * Instruments the allocation of (possibly) multiple arrays in one go (<code>multianewarray</code>).
	 * 
	 * Note that this snippet requires that bytecode verification is switched off (<code>-noverify</code>) as the newly
	 * created instances are passed to the runtime <em>prior</em> to initialization.
	 */
	@AfterReturning(marker = BytecodeMarker.class, args = "multianewarray", dynamicBypass = true)
	public static void multiArrayAllocated(DynamicContext dc, AllocationSiteStaticContext sc) {
		AbstractShadowHeapAnalysis.getInstance().onMultiArrayAllocation(dc.getStackValue(0, Object.class), sc.getAllocationSite());
	}

	/**
	 * This method assumes that <code>Class.newInstance</code> internally delegates to <code>Constructor.newInstance<code>
	 * and only instruments the latter. This assumption is true under OpenJDK and possibly other JREs.
	 */
	@AfterReturning(marker = BodyMarker.class, scope = "java.lang.Object java.lang.reflect.Constructor.newInstance(java.lang.Object[])", dynamicBypass = true)
	public static void objectAllocatedThroughClassNewInstance(DynamicContext dc, AllocationSiteStaticContext sc) {
		AbstractShadowHeapAnalysis.getInstance().onObjectAllocation(dc.getStackValue(0, Object.class), sc.getAllocationSiteForConstructorNewInstance());
	}

	@AfterReturning(marker = BodyMarker.class, scope = "java.lang.Object java.lang.reflect.Array.newInstance(java.lang.Class, int)", dynamicBypass = true)
	public static void objectAllocatedThroughArrayNewInstance(DynamicContext dc, AllocationSiteStaticContext sc) {
		AbstractShadowHeapAnalysis.getInstance().onObjectAllocation(dc.getStackValue(0, Object.class), sc.getAllocationSiteForArrayNewInstance());
	}

	@AfterReturning(marker = BodyMarker.class, scope = "java.lang.Object java.lang.reflect.Array.newInstance(java.lang.Class, int[])", dynamicBypass = true)
	public static void multiArrayAllocatedThroughArrayNewInstance(DynamicContext dc, AllocationSiteStaticContext sc) {
		AbstractShadowHeapAnalysis.getInstance().onObjectAllocation(dc.getStackValue(0, Object.class), sc.getAllocationSiteForMultiArrayNewInstance());
	}
}
