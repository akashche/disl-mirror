package ch.usi.dag.disl.test.pe;

public class TargetClass {

	private void private_method() {
		System.out.println("Inside TargetClass.private_method.");
	}

	public void public_method() {
		System.out.println("Inside TargetClass.public_method.");
	}

	public static void main(String[] args) {
		TargetClass t = new TargetClass();
		t.private_method();
		t.public_method();
	}
}
