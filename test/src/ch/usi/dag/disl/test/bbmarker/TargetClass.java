package ch.usi.dag.disl.test.bbmarker;

public class TargetClass {

	public void print(boolean branch) {
		String s = new String(new String(
				"This is the body of TargetClass.print"));

		System.out.println(s);

		if (branch) {
			System.out.println("branched");

			return;
		}

		System.out.println("not branched");
	}

	public static void main(String[] args) {
		TargetClass t = new TargetClass();
		System.out.println("=========call print(false)=========");
		t.print(false);
		System.out.println("=========call print(true) =========");
		t.print(true);
	}
}
