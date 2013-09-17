package ch.usi.dag.disl.test2.suite.after.app;

public class TargetClass {
	
	public void print(boolean flag) {
		try {
			System.out.println("app: TargetClass.print(..) - try:begin");

			if (flag) {
				String float_one = "1.0";
				int int_one = Integer.valueOf(float_one);
				System.out.println("app: UNREACHABLE " + int_one);
			}

			System.out.println("app: TargetClass.print(..) - try:end");
		} finally {
			System.out.println("app: TargetClass.print(..) - finally");
		}
	}

	// FIXME
	//	this main would fail the test
	//	could be fixed by adding app's code to the server's classpath	
	//
	//public static void main(String[] args) {
	//	try {
	//		TargetClass t = new TargetClass();
	//		System.out.println("app: main - .print(false)");
	//		t.print(false);
	//		System.out.println("app: main - .print(true)");
	//		t.print(true);
	//	} catch (Throwable e) {
	//		System.out.println("app: main - catch");
	//	}
	//}
	
	public static void main(String[] args) {
		TargetClass t = new TargetClass();
		System.out.println("app: TargetClass.main(..) - TargetClass.print(false)");
		t.print(false);
		System.out.println("app: TargetClass.main(..) - TargetClass.~print(false)");
		System.out.println("app: TargetClass.main(..) - TargetClass.print(true)");
		t.print(true);
		System.out.println("app: TargetClass.main(..) - TargetClass.~print(true)");

	}
}
