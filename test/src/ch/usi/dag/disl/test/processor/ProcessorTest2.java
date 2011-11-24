package ch.usi.dag.disl.test.processor;

import ch.usi.dag.disl.annotation.ArgsProcessor;
import ch.usi.dag.disl.annotation.SyntheticLocal;
import ch.usi.dag.disl.dynamiccontext.DynamicContext;
import ch.usi.dag.disl.processorcontext.ArgumentContext;

@ArgsProcessor
public class ProcessorTest2 {

	@SyntheticLocal
	public static String flag;

	public static void objPM(Object c, ArgumentContext ac, DynamicContext dc) {
		System.out.println("Testing dynamic context in processor");
		System.out.println("Accessed array index is "
				+ dc.stackValue(1, int.class));
		System.out.println("--------------------");

		DiSLClass.flag = "OMG this is for the End";
	}
}
