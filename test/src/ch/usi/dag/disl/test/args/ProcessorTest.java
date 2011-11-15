package ch.usi.dag.disl.test.args;

import ch.usi.dag.disl.annotation.ArgsProcessor;

@Processor
public class ProcessorTest {
	

	public static void objPM(int pos, int n, Object o) {
		DiSLClass.args[pos] = o;
	}
	

	public static void booleanPM(int pos, int n, boolean b) {
		DiSLClass.args[pos] = b;
	}

	
	public static void bytePM(int pos, int n, byte b) {
		DiSLClass.args[pos] = b;
	}
	
	
	public static void charPM(int pos, int n, char c) {
		DiSLClass.args[pos] = c;
	}
	
	
	public static void doublePM(int pos, int n, double d) {
		DiSLClass.args[pos] = d;
	}
	
	public static void floatPM(int pos, int n, float f) {
		DiSLClass.args[pos] = f;
	}
	
	
	public static void intPM(int pos, int n, int i) {
		DiSLClass.args[pos] = i;
	}

	
	public static void longPM(int pos, int n, long l) {
		DiSLClass.args[pos] = l;
	}

	
	public static void shortPM(int pos, int n, short s) {
		DiSLClass.args[pos] = s;
	}

	
}
