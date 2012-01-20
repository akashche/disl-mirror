package ch.usi.dag.disl.test.after2.runtime;

import java.util.Stack;

public class Analysis {
    public static void onExit(Stack<Integer> stack, int methodUID) {
        int topOfTheStack = stack.pop();
        if(topOfTheStack != methodUID) {
            System.err.println("=================================================");
            System.err.println("[ERROR] Inconsistent stack!");
            System.err.println("After method: " + methodUID);
            int size = stack.size();
            System.err.println("Stack[" + size + "]: " + topOfTheStack);
            for(int i = size - 1; i >= 0; i--) {
                System.err.println("Stack[" + i + "]: " + stack.elementAt(i));
            }
            System.err.println("=================================================");
            System.exit(-1);
        }
    }
}
