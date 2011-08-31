package ch.usi.dag.disl.test.guard;

import ch.usi.dag.disl.dislclass.annotation.Processor;

@Processor(guard=GuardNo.class)
public class ProcessorTest2 {

	public static void stringPM(int a, int b, Object c) {
		System.out.println("disabled processor for Object");
		System.out.println(a);
		System.out.println(b);
		System.out.println(c);
		System.out.println("--------------------");
	}
	
	public static void stringPM(int a, int b, String[] c) {
		System.out.println("disabled processor for String array");
		System.out.println(a);
		System.out.println(b);
		System.out.println(c.length);
		System.out.println("--------------------");
	}
}
