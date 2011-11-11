package ch.usi.dag.disl.test.map;

import ch.usi.dag.jborat.runtime.DynamicBypass;

public class TargetClass {
	
	
	  private static final byte [] fgLookupTable = new byte[128];
	  
	
	       private static final int ASCII_DIGIT_CHARACTERS = 0x20;      
	
	  
	         static {
		           
		           for (int i = '0'; i <= '9'; ++i) {
		               fgLookupTable[i] |= ASCII_DIGIT_CHARACTERS ; // tests |= (error because of dup2) 
		          }
		    
		       }
	
	public int value;
	public String stringValue;
	public static int staticValue = 0;
	
	
	
	public String toString() {
		System.out.println("THIS IS TO STRING ********");
		
		String thisString = "This is a new toString" + stringValue;
		return thisString;
	}
	
	@SuppressWarnings("unused")
	public void goo() {
		System.out.println("this object is " + this);
		synchronized(this) {
			System.out.println("This is a synchronized block");
			Object oo = new Object();
			synchronized(oo) {
				System.out.println("This is an embedded synchronized block");
			}
		}
		Object[] arr = new Object[] {new Object()};
		System.out.println("The array is " + arr);
		Object o = arr[0];
		System.out.println("The object in array is " + o);
		// test saload / satore  
		short[] sarr = new short[] { 10};
		short s = sarr[0];
		System.out.println("The short array is " + sarr + " at index 0");
		// test baload / bastore
		byte[] barr = new byte[] { 0 };
		byte b = barr[0];
		System.out.println("The byte array is " + barr + " at index 0");
		// test caload / castore
		char[] carr = new char[] {'h'};
		char c = carr[0];
		System.out.println("The char array is " + carr.toString() + " at index 0");
		// test faload / fastore
		float [] farr = new float[] { 1,0 };
		float f = farr[0];
		System.out.println("The char array is " + farr + " at index 0");
		// test daload /dastore
		double[] darr = new double[] { 1,0 };
		double d = darr[0];
		System.out.println("The double array is " + darr + " at index 0");
		// test iaload / iastore
	    int[] iarr = new int[] { 1 } ;
	    int i = iarr[0];
	    System.out.println("The int array is " + iarr + " at index 0");
	    // test laload / laload
	    long[] larr = new long[] { 1,1};
	    long l = larr[0];
	    System.out.println("The long array is " + larr + " at index 0");
	}
	
	@SuppressWarnings("unused")
	public void foo2() {
		value += 100;
		staticValue += 1000;
	    System.out.println("Value : " + value);
	    stringValue = "STRING VALUE";
	    System.out.println("String Value : " + stringValue);
	   
	    // Test anew Array
	    Thread[] threads = new Thread[100];
	    // test arraylength
	    if(threads.length < 0)
	    	System.out.println("impossible");
	    // Test multianewarray
	    int[][] multi = new int[10][10];
	    goo();
	    hoo(); // test synchronized method...
	}
	
	public synchronized void hoo() {
		System.out.println("This is synchronized method " + this);
	}
	
	@SuppressWarnings("unused")
	public void foo() {
		String s = stringValue; // getfield..
		Size size = new Size(new String());
	}
	
	
	
	public static void main(String[] args) {
		System.out.println("BYPASS " + DynamicBypass.get());
		TargetClass c = new TargetClass();
		System.out.println("THE OBJECT " + c + "BYPASS " + DynamicBypass.get());
		c.foo();
		c.foo2();
		
		
	}
	
	// test putfield before object init
	class Size {
		
		final String aa;
		Size(String a) {
			this.aa = a;
		}
	}
}