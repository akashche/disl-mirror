package ch.usi.dag.disl.test.after4;

public class TargetClass {

	public void print(boolean flag) {
		try {
			System.out.println("try-clause");

			if (flag) {
				String float_one = "1.0";

				int int_one = Integer.valueOf(float_one);
				System.out.println(int_one + "This should not be printed!");
			}

			System.out.println("normal return");
		} finally {
			System.out.println("finally-clause");
		}
	}

	public static void main(String[] args) {
		TargetClass t = new TargetClass();
		System.out.println("=========call print(false)=========");
		t.print(false);
		System.out.println("=========call print(true) =========");
		t.print(true);
	}
}
