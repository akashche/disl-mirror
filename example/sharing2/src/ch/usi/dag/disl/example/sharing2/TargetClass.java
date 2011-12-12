package ch.usi.dag.disl.example.sharing2;




public class TargetClass extends Thread {
	private int x;
	
	public void run(){
		System.err.println("TEST!" + x++);
	}
	
	public TargetClass(){
		x = 5;
	}



	public static void main(String[] args){
		for (int i=0; i< 10; i++){			
			TargetClass tc_i = new TargetClass();
			tc_i.start();
		}

			
		System.out.println("TargetClass successfully executed!");
	}
	
}