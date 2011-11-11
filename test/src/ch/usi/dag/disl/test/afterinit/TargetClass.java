package ch.usi.dag.disl.test.afterinit;

public class TargetClass {
	
	public TargetClass() {
		
		System.out.println("Constructing TargetClass");
	}
	
	static class TargetClass2 extends TargetClass {
		
		public TargetClass2() {
			
			System.out.println("Constructing TargetClass2");
		}
	
		public void method() {
			System.out.println("Construction end");
		}
	}

	public static void main(String[] args) {
		TargetClass2 t = new TargetClass2();
		t.method();
	}
}
