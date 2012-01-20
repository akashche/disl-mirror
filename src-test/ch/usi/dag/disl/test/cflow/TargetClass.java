package ch.usi.dag.disl.test.cflow;


public class TargetClass {
	

	public void foo() {
		System.out.println("foo() called. will call goo()");
		goo();
	}
	
	public  void goo() {
		System.out.println("goo called");
	}
	
	public void hoo() {
		System.out.println("hoo should not be in cflow of foo()");
		System.out.println("now calling goo, should not be in cflow neither");
		goo();
	}
	
	public static void main(String[] args) {
		TargetClass c = new TargetClass();
		c.foo();
		c.hoo();
	}
}
