package ch.usi.dag.disl.test.staticfield;
public class TargetClass {
	
	public void print() {
		System.out.println("This is the body of TargetClass.print");
	}

	public static void main(String[] args) {
		TargetClass t = new TargetClass();
		t.print();
	}
}
