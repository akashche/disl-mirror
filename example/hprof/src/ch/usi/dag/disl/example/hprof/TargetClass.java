package ch.usi.dag.disl.example.hprof;


public class TargetClass {

	public static void main(String[]args) {
		System.err.println("Hello!!");
		for(int i=0; i<10000;i++) {
			new Object();  
		}
	}
}








