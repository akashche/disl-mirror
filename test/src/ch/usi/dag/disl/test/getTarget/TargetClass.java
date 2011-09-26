package ch.usi.dag.disl.test.getTarget;

class A {
	public void foo() {
		B.callFoo(0, 1);
	}
}

class B {
	public void foo(int i, int j) {
		System.out.println("Inside b.foo");
	}

	public static void callFoo(int i, int j) {
		B b = new B();
		b.foo(i, j);
	}
}

public class TargetClass {

	public static void main(String[] args) {
		A a = new A();
		a.foo();
	}
}
