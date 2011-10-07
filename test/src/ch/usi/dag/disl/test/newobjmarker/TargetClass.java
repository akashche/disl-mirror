package ch.usi.dag.disl.test.newobjmarker;

public class TargetClass {
	
	public final String printMe;
	
	TargetClass(String printMe) {
		this.printMe = printMe;
	}

	public static void main(String[] args) {
		
		TargetClass b = new TargetClass("Hi");
		// no warning :)
		b.toString();
	}
}
