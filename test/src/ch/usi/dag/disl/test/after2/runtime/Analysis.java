package ch.usi.dag.disl.test.after2.runtime;

import java.util.LinkedList;
import java.util.Stack;

public class Analysis {
	
	public static void log(LinkedList<String> thisLog, String msg) {
		
		thisLog.addFirst(Thread.currentThread().getId() + " " + msg);
        if(thisLog.size() > 100) {
        	thisLog.removeLast();
        }
	}
	
    public static void onExit(Stack<Integer> thisStack, int id, LinkedList<String> thisLog, String msg) {
    	
    	int topOfTheStack = thisStack.pop();
    	
		if (topOfTheStack != id) {
			
	    	synchronized(System.err) {
			
				System.err.println("=================================================");
				System.err.println("[ERROR] Inconsistent stack!");
				System.err.println("After method: " + id);
				int size = thisStack.size();
				System.err.println("Stack[" + size + "]: " + topOfTheStack);
				for (int i = size - 1; i >= size - 20; i--) {
					System.err.println("Stack[" + i + "]: " + thisStack.elementAt(i));
				}
				System.err.println("=================================================");
				
				for(int i = 0; i < thisLog.size(); ++i) {
					System.err.println(thisLog.get(i));
				}
				
				System.err.println("=================================================");
				
				System.exit(-1);
	    	}
		}
		
		log(thisLog, msg + id);
    }
}
