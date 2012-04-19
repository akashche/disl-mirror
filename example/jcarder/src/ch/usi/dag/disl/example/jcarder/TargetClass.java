package ch.usi.dag.disl.example.jcarder;

public class TargetClass {
//	Object myLock = new Object();
//	Object myLock2 = new Object();
//
//	void fum() {
//		synchronized (myLock) {
//			System.err.println("Holding a lock on " + myLock.hashCode());
//		}
//		synchronized (myLock2) {
//			System.err.println("Holding a lock on " + myLock2.hashCode());
//		};
//		
//	}
//
//	void fie() {
//		synchronized (myLock2) {
//			System.err.println("Holding a lock on " + myLock2.hashCode());
//		};
//		synchronized (myLock) {
//			System.err.println("Holding a lock on " + myLock.hashCode());
//		}
//	}
//
//	synchronized void  foo() {
//		fum();
//	}
//
//	public static void main(String args[]) {
//		System.err.println("TargetClass is starting!");
//		TargetClass tc = new TargetClass();
//		tc.fum();
//		tc.foo();
//		tc.fie();
//		System.err.println("TargetClass finished!");
//	}
//	
//	public synchronized String toString() {
//		return new String(new byte[3], 0, 1);
//	}
//	private static  synchronized void writeObject(java.io.ObjectOutputStream s)
//			throws java.io.IOException {
//		java.io.ObjectOutputStream.PutField fields = s.putFields();
//		fields.put("value", 1);
//		fields.put("count", 2);
//		fields.put("shared", false);
//		s.writeFields();
//	}

    Object myLock = new Object();

    void fum() {
        synchronized (myLock) {
        }
    }

    void fie() {
        fum();
    }

    synchronized void foo() {
        fie();
    }

    public static void main(String args[]) {
        new TargetClass().foo();
    }
}


