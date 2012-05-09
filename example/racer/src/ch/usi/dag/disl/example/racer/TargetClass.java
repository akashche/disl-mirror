package ch.usi.dag.disl.example.racer;

public class TargetClass extends Thread {
static int shared;
static int shared_protected;
int not_shared;
	

		public void run() {
			System.err.println(shared++);
			synchronized (TargetClass.class) {
				System.err.println(shared_protected++);
			}
			System.err.println(not_shared++);
		}
	
	

	
	public static void main(String[]args){
		
		TargetClass thread1 = new TargetClass();
		TargetClass thread2 = new TargetClass();

		thread1.start();
		thread2.start();

	//	new TargetClass().test();
		
	}

//	public void test() {
//		B b = new B();
//		A a = b;
//
//		System.out.println("TEST1");
//		b.set(3);
//		a.set(3);
//
//		System.out.println("TEST2");
//		a = new A();
//		b = new B();
//		b.set(3);
//		a.set(3);
//
//		System.out.println("TEST3");
//		a = new A();
//		C c = new C();
//		a.set(3);
//		c.set(5);
//		System.out.println("a: " + a.get());
//		System.out.println("c: " + c.get());
//	}
//
//	private class A {
//		int x;
//
//		void set(int y) { x = y; }
//		int get() { return x; }
//	}
//
//	private class B extends A {
//		void set(int y) { x = y; }
//		int get() { return x; }
//	}

//	private class C extends A {
//		int x;
//
//		void set(int y) { x = y; }
//		int get() { return x; }
//	}
}
