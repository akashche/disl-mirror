package ch.usi.dag.disl.test.dynamicinfo;

public class TargetClass {

	public void test1() {
		int a = 1;
		int b = 2;
		System.out.println(a++ - ++b);
	}

	public int test2(boolean flag) {
		return flag ? 1 : -1;
	}

	public void test3(double d, int i) {
		d += 1;
		System.out.println("d is now " + d);
		i -= 1;
		System.out.println("i is now " + i);
		System.out.println(d - i);
	}

	public static void main(String[] args) {
		TargetClass t = new TargetClass();
		System.out.println("Calling t.test(), which prints -2");
		t.test1();
		System.out.println("Calling t.test2(true), which returns 1");
		t.test2(true);
		System.out.println("Calling t.test2(true), which returns -1");
		t.test2(false);
		System.out.println("Calling t.test3(1.0, 2)");
		t.test3(1.0, 2);
	}
}
