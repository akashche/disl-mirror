package ch.usi.dag.disl.test.dispatch2;
public class TargetClass {
	
	public static void main(String[] args) throws InterruptedException {

		long start = System.nanoTime();
		
		int COUNT = 10000000;
		
		TargetClass ta[] = new TargetClass[COUNT];
		
		int i;
		
		for(i = 0; i < COUNT; ++i) {
			ta[i] = new TargetClass();
		}
		
		System.out.println("Allocated " + i + " objects in "
				+ (System.nanoTime() - start) / 1000000 + " ms");
	}
}
