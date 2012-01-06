package ch.usi.dag.disl.test.pe3;

public class TargetClass {
	
	int i;

	private void private_method() {
		System.out.println("Inside TargetClass.private_method.");
	}

	public void public_method() {
		i = 1;
		System.out.println(i);
	}

	public static void main(String[] args) {
		TargetClass t = new TargetClass();
		t.private_method();
		t.public_method();
	}
}
