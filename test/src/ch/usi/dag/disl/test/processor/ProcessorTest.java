package ch.usi.dag.disl.test.processor;

import ch.usi.dag.disl.dislclass.annotation.Processor;
import ch.usi.dag.disl.dislclass.annotation.SyntheticLocal;

@Processor
public class ProcessorTest {

	@SyntheticLocal
	public static String flag;
	
	public static void objPM(int a, int b, Object c) {
		System.out.println("processor for object");
		System.out.println(a);
		System.out.println(b);
		System.out.println(c);
		System.out.println("--------------------");
		
		DiSLClass.flag = "OMG this is for the End";
	}

	public static void intPM(int a, int b, int c) {
		System.out.println("processor for int");
		System.out.println(a);
		System.out.println(b);
		System.out.println(c);
		System.out.println("--------------------");
		
		flag = "Processor flag for the End";
	}

	public static void longPM(int a, int b, long c) {
		System.out.println("processor for long");
		System.out.println(a);
		System.out.println(b);
		System.out.println(c);
		System.out.println("--------------------");
	}

	public static void doublePM(int a, int b, double c) {
		System.out.println("processor for double");
		System.out.println(a);
		System.out.println(b);
		System.out.println(c);
		System.out.println("--------------------");
	}

	public static void stringPM(int a, int b, String c) {
		System.out.println("processor for String");
		System.out.println(a);
		System.out.println(b);
		System.out.println(c);
		System.out.println("--------------------");
	}
	
	public static void stringPM(int a, int b, String[] c) {
		System.out.println("processor for String array");
		System.out.println(a);
		System.out.println(b);
		System.out.println(c.length);
		System.out.println("--------------------");
	}
}
