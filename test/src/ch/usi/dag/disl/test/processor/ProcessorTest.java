package ch.usi.dag.disl.test.processor;

import ch.usi.dag.disl.annotation.ProcessAlso;
import ch.usi.dag.disl.annotation.ProcessAlso.Type;
import ch.usi.dag.disl.annotation.ArgsProcessor;
import ch.usi.dag.disl.annotation.SyntheticLocal;
import ch.usi.dag.disl.processor.ArgumentContext;

@ArgsProcessor
public class ProcessorTest {

	@SyntheticLocal
	public static String flag;
	
	public static void objPM(Object c, ArgumentContext ac) {
		System.out.println("processor for object");
		System.out.println(ac.position());
		System.out.println(ac.totalCount());
		System.out.println(ac.type());
		System.out.println(c);
		System.out.println("--------------------");
		
		DiSLClass.flag = "OMG this is for the End";
	}
	
	@ProcessAlso(types={Type.SHORT, Type.BYTE, Type.BOOLEAN})
	public static void intPM(int c, ArgumentContext ac) {
		System.out.println("processor for int");
		System.out.println(ac.position());
		System.out.println(ac.totalCount());
		System.out.println(ac.type());
		System.out.println("--------------------");
		
		flag = "Processor flag for the End";
	}

	public static void longPM(long c, ArgumentContext ac) {
		System.out.println("processor for long");
		System.out.println(ac.position());
		System.out.println(ac.totalCount());
		System.out.println(ac.type());
		System.out.println("--------------------");
	}

	public static void doublePM(double c, ArgumentContext ac) {
		System.out.println("processor for double");
		System.out.println(ac.position());
		System.out.println(ac.totalCount());
		System.out.println(ac.type());
		System.out.println("--------------------");
	}
}
