package ch.usi.dag.disl.test.tryclause;
public class TargetClass {
	
	public void print(boolean branch) {
		try{
			if (branch){
				throw new Exception();
			}
			
			System.out.println("Without exception");
		}catch (Exception e) {
			System.out.println("Exception handler");
		}
	}

	public static void main(String[] args) {
		TargetClass t = new TargetClass();
		System.out.println("+++call print(false)+++");
		t.print(false);
		System.out.println("+++call print(true)+++");
		t.print(true);
	}
}
