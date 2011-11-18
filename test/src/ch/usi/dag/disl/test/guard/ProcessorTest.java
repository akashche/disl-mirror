package ch.usi.dag.disl.test.guard;

import ch.usi.dag.disl.annotation.Guarded;
import ch.usi.dag.disl.annotation.ArgsProcessor;
import ch.usi.dag.disl.processor.ArgumentContext;

@ArgsProcessor
public class ProcessorTest {

	@Guarded(guard=GuardYes.class)
	public static void stringPM(Object c, ArgumentContext ac) {
		System.out.println("processor for Object");
		System.out.println(ac.position());
		System.out.println(ac.totalCount());
		System.out.println(c);
		System.out.println("--------------------");
	}
	
	@Guarded(guard=GuardNo.class)
	public static void stringPM(int c, ArgumentContext ac) {
		System.out.println("processor for int");
		System.out.println(ac.position());
		System.out.println(ac.totalCount());
		System.out.println("--------------------");
	}
}
