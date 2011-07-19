package ch.usi.dag.disl.test.catchclause;

public class TargetClass {

	public void print(boolean branch) {
		try {
			if (branch) {
				throw new Exception();
			}

			Integer.valueOf("1.0");
		} catch (NumberFormatException e) {
			System.out.println("NumberFormatException handler");
		} catch (Exception e) {
			System.out.println("Exception handler");
		}
	}

	public static void main(String[] args) {
		TargetClass t = new TargetClass();
		System.out.println("+++call print(false)+++");
		t.print(false);
		System.out.println("+++call print(true)+++");
		t.print(true);
	}
}
