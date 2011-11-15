package ch.usi.dag.disl.test.guard;

import ch.usi.dag.disl.annotation.ArgsProcessor;

@Processor(guard=GuardNo.class)
public class ProcessorTest2 {

	public static void stringPM(int a, int b, Object c) {
		System.out.println("disabled processor for Object");
		System.out.println(a);
		System.out.println(b);
		System.out.println(c);
		System.out.println("--------------------");
	}
	
	public static void stringPM(int a, int b, int c) {
		System.out.println("disabled processor for int");
		System.out.println(a);
		System.out.println(b);
		System.out.println("--------------------");
	}
}
