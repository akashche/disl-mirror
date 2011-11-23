package ch.usi.dag.disl.test.args;

import ch.usi.dag.disl.annotation.ArgsProcessor;
import ch.usi.dag.disl.processorcontext.ArgumentContext;

@ArgsProcessor
public class ProcessorTest {
	

	public static void objPM(Object o, ArgumentContext ac) {
		DiSLClass.args[ac.position()] = o;
	}
	

	public static void booleanPM(boolean b, ArgumentContext ac) {
		DiSLClass.args[ac.position()] = b;
	}

	
	public static void bytePM(byte b, ArgumentContext ac) {
		DiSLClass.args[ac.position()] = b;
	}
	
	
	public static void charPM(char c, ArgumentContext ac) {
		DiSLClass.args[ac.position()] = c;
	}
	
	
	public static void doublePM(double d, ArgumentContext ac) {
		DiSLClass.args[ac.position()] = d;
	}
	
	public static void floatPM(float f, ArgumentContext ac) {
		DiSLClass.args[ac.position()] = f;
	}
	
	
	public static void intPM(int i, ArgumentContext ac) {
		DiSLClass.args[ac.position()] = i;
	}

	
	public static void longPM(long l, ArgumentContext ac) {
		DiSLClass.args[ac.position()] = l;
	}

	
	public static void shortPM(short s, ArgumentContext ac) {
		DiSLClass.args[ac.position()] = s;
	}

	
}
