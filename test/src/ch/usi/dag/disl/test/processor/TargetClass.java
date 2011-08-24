package ch.usi.dag.disl.test.processor;

public class TargetClass {
	
	public void method1() {
		System.out.println("This is the body of TargetClass.method1");
	}
	
	public void method2(int i, long l, float d) {
		System.out.println("This is the body of TargetClass.method2");
	}
	
	public void method3(Object obj, String string) {
		System.out.println("This is the body of TargetClass.method3");
	}
	
	public void method3(long l, double d, Object obj) {
		System.out.println("This is the body of TargetClass.method3-1");
	}
	
	public void method4(String[] sa, int[] ia) {
		System.out.println("This is the body of TargetClass.method4");
	}

	public static void main(String[] args) {
		TargetClass t = new TargetClass();
		t.method1();
		t.method2(1, 2, 3);
		t.method3("object", "string");
		t.method3(1, 2, "object");
		t.method4(new String[1], new int[1]);
	}
}
