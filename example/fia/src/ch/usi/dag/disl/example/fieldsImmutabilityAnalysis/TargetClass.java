package ch.usi.dag.disl.example.fieldsImmutabilityAnalysis;

import java.util.ArrayList;
import java.util.List;



public class TargetClass {
	private static List<Integer> list;

	static{
		list = new ArrayList<Integer>();
		
	}

	private class Inner extends TargetClass {
		private int inner_mutable_1;
		private int iner_immutable_1;
		
		public Inner(){
			this.iner_immutable_1=2;
		}
		
		public void setTargetClassMutableValue(int value){
			super.mutable_1=3;
		}
	}
	
	private int immutable_1;
	private int immutable_2;
	private int mutable_1;
	
	private void useAnonymousClass(){
		Anomymous ac = new Anomymous(){
			int anonymous_immutable =4;
			void setMutableValue(int value){
				this.anonymous_immutable = value;
			}
		};
	}
	
	
	
	public TargetClass(){
		immutable_1=3;
		init(75);
	}

	private void init(int value) {
		immutable_2=value;
	}

	public static void main(String[] args){
		TargetClass tc = new TargetClass();
		tc.mutable_1=21;
		TargetClass.Inner ic = tc.new Inner();
		ic.setTargetClassMutableValue(3);
		tc.useAnonymousClass();
		System.out.println("TargetClass successfully executed!");
	}
	public interface Anomymous {
	
	}
	
}