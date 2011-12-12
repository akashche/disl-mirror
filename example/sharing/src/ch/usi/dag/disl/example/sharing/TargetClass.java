package ch.usi.dag.disl.example.sharing;

import java.util.concurrent.atomic.AtomicInteger;

public class TargetClass extends Thread {
    private final AtomicInteger x = new AtomicInteger();

    public TargetClass(String str){
        super(str);
    }

    public void run(){
        System.out.println("TEST! x: " + x.incrementAndGet());
    }

    public static void main(String[] args){
        for (int i = 0; i < 1; i++) {			
            TargetClass tc = new TargetClass(String.valueOf(i));
            tc.start();
        }
    }
}
