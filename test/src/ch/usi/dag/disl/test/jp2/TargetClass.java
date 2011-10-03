package ch.usi.dag.disl.test.jp2;

import ch.usi.dag.disl.test.jp2.TargetClass;
import ch.usi.dag.disl.test.jp2.runtime.CCTNode;

public class TargetClass extends AbstractTargetClass {
	
	public static int staticValue;
	
	public void hoo(int i) {
		if(i==0)
			return;
		else
			hoo(--i);
	}
	
	public void foo(boolean t) {
		
		int i = 0;
		if(t) {
			i++;
		}else{
			i--;
		}
		staticValue =0;
		for(i=0; i<5; i++)
			hoo(5);	
	}
	
	public static void main(String[] args) {
		
		TargetClass t = new TargetClass();
		t.foo(true);
	}
}