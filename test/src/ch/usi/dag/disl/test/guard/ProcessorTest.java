package ch.usi.dag.disl.test.guard;

import ch.usi.dag.disl.dislclass.annotation.Guarded;
import ch.usi.dag.disl.dislclass.annotation.Processor;

@Processor(guard=GuardYes.class)
public class ProcessorTest {

	@Guarded(guard=GuardYes.class)
	public static void stringPM(int a, int b, Object c) {
		System.out.println("processor for Object");
		System.out.println(a);
		System.out.println(b);
		System.out.println(c);
		System.out.println("--------------------");
	}
	
	@Guarded(guard=GuardNo.class)
	public static void stringPM(int a, int b, String[] c) {
		System.out.println("processor for String array");
		System.out.println(a);
		System.out.println(b);
		System.out.println(c.length);
		System.out.println("--------------------");
	}
}
