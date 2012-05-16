package ch.usi.dag.disl.example.senseo;

public class TargetClass {
	 
	public static final int UNINITIALIZED = -1;
	 
		private int numberOfArgs = UNINITIALIZED;
		
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
	
	 public void setArgs(int num)
     {
        this.numberOfArgs = num;
    }
	
	
	public static void main(String[] args) {
		
		TargetClass t = new TargetClass();
		t.method1();
		t.method2(1, 2, 3);
		t.method3("object", "string");
		t.method3("object2", "string2");
		t.method3(t, "THIS IS THE REF TO THIS");
		t.method3(1, 2, "object");
		t.method4(new String[] {"hello"}, new int[] {1,2,3,4,5});
		t.setArgs(100);
	
	}
	
}


