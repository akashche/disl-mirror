package ch.usi.dag.disl.test.symbolic;

public class TargetClass {
	
	public static void lock(){
		System.out.println("lock");
	}
	
	public static void unlock(){
		System.out.println("unlock");
	}
			
	public void print(int num_proc){
		if (num_proc > 1){
			lock();
		}
		
		num_proc--;
		
		if (num_proc > 1){
			unlock();
		}
				
		if (num_proc > 1){
			lock();
		}
		
		num_proc--;
		
		if (num_proc > 1){
			unlock();
		}
	}
	
	public static void main(String[] args) {
		TargetClass t = new TargetClass();
		t.print(5);
	}
}
