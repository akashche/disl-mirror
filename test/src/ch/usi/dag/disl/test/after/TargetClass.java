package ch.usi.dag.disl.test.after;

public class TargetClass {

	public void print(boolean flag) {
		try {
			System.out.println("Stepping into try-catch block.");
			
			if (flag){
				String float_one = "1.0";
				
				int int_one = Integer.valueOf(float_one);
				System.out.println(int_one + "This should not be printed!");
			}
			
			System.out.println("After returning!");
			return;
		} catch (Exception e) {
			System.out.println("This is the default handler of the target class.");
			return;
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
