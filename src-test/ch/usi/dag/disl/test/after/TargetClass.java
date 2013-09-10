package ch.usi.dag.disl.test.after;

public class TargetClass {

	public void print(boolean flag) {
		try {
			System.out.println("app: try-clause");

			if (flag) {
				String float_one = "1.0";
				int int_one = Integer.valueOf(float_one);
				System.out.println("app: " + int_one + "This should not be printed!");
			}

			System.out.println("app: normal return");
		} finally {
			System.out.println("app: finally-clause");
		}
	}

	public static void main(String[] args) {
		TargetClass t = new TargetClass();
		System.out.println("app: call print(false)");
		t.print(false);
		System.out.println("app: call print(true)");
		t.print(true);
	}
}
