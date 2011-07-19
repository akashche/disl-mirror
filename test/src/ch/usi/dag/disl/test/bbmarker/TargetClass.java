package ch.usi.dag.disl.test.bbmarker;

public class TargetClass {

	public void print(boolean branch) {
		if (branch){
			System.out.println("true");
		}else{
			System.out.println("flase");
		}
		
		return;
	}

	public static void main(String[] args) {
		TargetClass t = new TargetClass();
		System.out.println("=========call print(false)=========");
		t.print(false);
		System.out.println("=========call print(true) =========");
		t.print(true);
	}
}
