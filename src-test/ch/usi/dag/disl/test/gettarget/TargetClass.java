package ch.usi.dag.disl.test.gettarget;

class TargetClassA {
	public void foo() {
		TargetClassB.callFoo(0, 1);
	}
}

class TargetClassB {
	public void foo(int i, int j) {
		System.out.println("Inside b.foo");
	}

	public static void callFoo(int i, int j) {
		TargetClassB b = new TargetClassB();
		b.foo(i, j);
	}
}

public class TargetClass {

	public static void main(String[] args) {
		TargetClassA a = new TargetClassA();
		a.foo();
	}
}
