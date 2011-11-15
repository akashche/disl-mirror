package ch.usi.dag.disl.test.guard;

import ch.usi.dag.disl.annotation.Guarded;
import ch.usi.dag.disl.annotation.ArgsProcessor;

@ArgsProcessor(guard=GuardYes.class)
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
	public static void stringPM(int a, int b, int c) {
		System.out.println("processor for int");
		System.out.println(a);
		System.out.println(b);
		System.out.println("--------------------");
	}
}
