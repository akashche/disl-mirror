package ch.usi.dag.disl.example.jraf2;

public class TargetClass {
	
	public void doit() {
		return;
	}
	
	public int crash() {
		return 100/0;
	}
	
	public void method(int max) {
		int total = 100;
		for(int i = 0 ; i < max; i++) {
			total ++;
		}
		
		for(int i = 0 ; i < max; i++) {
			total ++;
			doit();
		}
		
		for(int i=0 ; i < max; i++) {
			try {
				crash();
			}catch(Exception e) {
				System.out.println("This is the EXCEPTION HANDLER");
			}
		}
		
		System.out.println(total);
	}
	
	
	public static void main(String[] args) {
		TargetClass t = new TargetClass();
		t.method(1000);
	}
}
