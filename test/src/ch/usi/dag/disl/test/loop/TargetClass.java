package ch.usi.dag.disl.test.loop;

public class TargetClass {

	public void print() {
		
		for (int i = 0; i<4; i++){
			System.out.println(i);
		}
		
		return;
	}

	public static void main(String[] args) {
		TargetClass t = new TargetClass();
		t.print();
	}
}
