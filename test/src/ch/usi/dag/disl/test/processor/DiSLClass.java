package ch.usi.dag.disl.test.processor;

import ch.usi.dag.disl.ProcessorHack;
import ch.usi.dag.disl.annotation.AfterReturning;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.annotation.SyntheticLocal;
import ch.usi.dag.disl.snippet.marker.BodyMarker;
import ch.usi.dag.disl.staticinfo.analysis.ContextInfoo;

public class DiSLClass {

	@SyntheticLocal
	static String flag;

	@ProcessorHack.Processor(type = void.class)
	public static void processor1() {
		// this will be called before any processor

		flag = "OMG this is for postCondition";
	}

	@ProcessorHack.Processor(type = Object.class)
	public static void processor2(int a, int b, Object c) {
		System.out.println("processor for object");
		System.out.println(a);
		System.out.println(b);
		System.out.println(c);
		System.out.println("--------------------");
	}

	@ProcessorHack.Processor(type = int.class)
	public static void processor3(int a, int b, int c) {
		System.out.println("processor for int");
		System.out.println(a);
		System.out.println(b);
		System.out.println(c);
		System.out.println("--------------------");
	}

	@ProcessorHack.Processor(type = long.class)
	public static void processor4(int a, int b, long c) {
		System.out.println("processor for long");
		System.out.println(a);
		System.out.println(b);
		System.out.println(c);
		System.out.println("--------------------");
	}

	@ProcessorHack.Processor(type = double.class)
	public static void processor5(int a, int b, double c) {
		System.out.println("processor for double");
		System.out.println(a);
		System.out.println(b);
		System.out.println(c);
		System.out.println("--------------------");
	}
	

	@ProcessorHack.Processor(type = String.class)
	public static void processor6(int a, int b, String c) {
		System.out.println("processor for String");
		System.out.println(a);
		System.out.println(b);
		System.out.println(c);
		System.out.println("--------------------");
	}

	@Before(marker = BodyMarker.class, order = 0, scope = "TargetClass.*")
	public static void preCondition() {
		System.out.println("Method " + ContextInfo.getMethodName(null) + ":");
		System.out.println(flag);
	}
	
	@AfterReturning(marker = BodyMarker.class, order = 1, scope = "TargetClass.*")
	public static void postCondition() {
		System.out.println("Again");
		System.out.println(flag);
		System.out.println("This is the end of " + ContextInfo.getMethodName(null));
	}
}
