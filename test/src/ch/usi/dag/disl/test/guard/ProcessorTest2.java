package ch.usi.dag.disl.test.guard;

import ch.usi.dag.disl.annotation.ArgsProcessor;
import ch.usi.dag.disl.processor.ArgumentContext;

@ArgsProcessor
public class ProcessorTest2 {

	public static void stringPM(Object c, ArgumentContext ac) {
		System.out.println("disabled processor for Object");
		System.out.println(ac.position());
		System.out.println(ac.totalCount());
		System.out.println(c);
		System.out.println("--------------------");
	}
	
	public static void stringPM(int c, ArgumentContext ac) {
		System.out.println("disabled processor for int");
		System.out.println(ac.position());
		System.out.println(ac.totalCount());
		System.out.println("--------------------");
	}
}
