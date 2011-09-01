package ch.usi.dag.disl.test.senseo;

import ch.usi.dag.disl.dislclass.annotation.Processor;
import ch.usi.dag.disl.test.senseo.runtime.arguments.Boolean;
import ch.usi.dag.disl.test.senseo.runtime.arguments.Byte;
import ch.usi.dag.disl.test.senseo.runtime.arguments.Char;
import ch.usi.dag.disl.test.senseo.runtime.arguments.Double;
import ch.usi.dag.disl.test.senseo.runtime.arguments.Float;
import ch.usi.dag.disl.test.senseo.runtime.arguments.Int;
import ch.usi.dag.disl.test.senseo.runtime.arguments.Long;
import ch.usi.dag.disl.test.senseo.runtime.arguments.Null;
import ch.usi.dag.disl.test.senseo.runtime.arguments.Short;
import ch.usi.dag.disl.test.senseo.runtime.arguments.String;


@Processor
public class ProcessorTest {
	

	public static void objPM(int pos, int n, Object o) {
//		System.out.println("processor for object");
//		System.out.println(pos);
//		System.out.println(n);
//		System.out.println(o);
//		System.out.println("--------------------");
//		System.out.println(" THIS ANALYSIS " + DiSLClass.thisAnalysis);
		DiSLClass.thisAnalysis.profileArgument((o==null)?Null.class: o.getClass(), pos);
		
	}
	

	public static void booleanPM(int pos, int n, boolean b) {
//		System.out.println("processor for boolean");
//		System.out.println(pos);
//		System.out.println(n);
//		System.out.println(b);
//		System.out.println("--------------------");
//		System.out.println(" THIS ANALYSIS " + DiSLClass.thisAnalysis);
		DiSLClass.thisAnalysis.profileArgument(Boolean.class, pos);
	}

	
	public static void bytePM(int pos, int n, byte b) {
//		System.out.println("processor for byte");
//		System.out.println(pos);
//		System.out.println(n);
//		System.out.println(b);
//		System.out.println("--------------------");
//		System.out.println(" THIS ANALYSIS " + DiSLClass.thisAnalysis);
		DiSLClass.thisAnalysis.profileArgument(Byte.class, pos);
	}
	
	
	public static void charPM(int pos, int n, char c) {
//		System.out.println("processor for char");
//		System.out.println(pos);
//		System.out.println(n);
//		System.out.println(c);
//		System.out.println("--------------------");
//		System.out.println(" THIS ANALYSIS " + DiSLClass.thisAnalysis);
		DiSLClass.thisAnalysis.profileArgument(Char.class, pos);
		
	}
	
	
	public static void doublePM(int pos, int n, double d) {
//		System.out.println("processor for double");
//		System.out.println(pos);
//		System.out.println(n);
//		System.out.println(d);
//		System.out.println("--------------------");
//		System.out.println(" THIS ANALYSIS " + DiSLClass.thisAnalysis);
		DiSLClass.thisAnalysis.profileArgument(Double.class, pos);
	
	}
	
	public static void floatPM(int pos, int n, float f) {
//		System.out.println("processor for float");
//		System.out.println(pos);
//		System.out.println(n);
//		System.out.println(f);
//		System.out.println("--------------------");
//		System.out.println(" THIS ANALYSIS " + DiSLClass.thisAnalysis);
		DiSLClass.thisAnalysis.profileArgument(Float.class, pos);
	
	}
	
	
	public static void intPM(int pos, int n, int i) {
//		System.out.println("processor for int");
//		System.out.println(pos);
//		System.out.println(n);
//		System.out.println(i);
//		System.out.println("--------------------");
	//System.out.println(" THIS ANALYSIS " + DiSLClass.thisAnalysis + " int value " + i);
	//	int ii = 0;
		DiSLClass.thisAnalysis.profileArgument(Int.class, pos);
		
	}

	
	public static void longPM(int pos, int n, long l) {
//		System.out.println("processor for long");
//		System.out.println(pos);
//		System.out.println(n);
//		System.out.println(l);
//		System.out.println("--------------------");
//		System.out.println(" THIS ANALYSIS " + DiSLClass.thisAnalysis);
		DiSLClass.thisAnalysis.profileArgument(Long.class, pos);
	
	}

	
	public static void shortPM(int pos, int n, short s) {
//		System.out.println("processor for short");
//		System.out.println(pos);
//		System.out.println(n);
//		System.out.println(s);
//		System.out.println("--------------------");
//		System.out.println(" THIS ANALYSIS " + DiSLClass.thisAnalysis);
		DiSLClass.thisAnalysis.profileArgument(Short.class, pos);
		
	}

	

	public static void stringPM(int pos, int n, java.lang.String s) {
//		System.out.println("processor for String");
//		System.out.println(pos);
//		System.out.println(n);
//		System.out.println(s);
//		System.out.println("--------------------");
//		System.out.println(" THIS ANALYSIS " + DiSLClass.thisAnalysis);
		DiSLClass.thisAnalysis.profileArgument(String.class, pos);
		
	}
	
}
