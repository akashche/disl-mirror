package ch.usi.dag.disl.test.guard;

public class TargetClass {
	
	public void method1(Object o, int i) {
		System.out.println("This is the body of TargetClass.method1");
	}
	
	public static void main(String[] args) {

		TargetClass t = new TargetClass();
		t.method1(new Integer(0), 0);
		
		System.out.println("Testing method");
	}
}
