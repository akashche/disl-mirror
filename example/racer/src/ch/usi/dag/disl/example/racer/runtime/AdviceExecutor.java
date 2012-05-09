package ch.usi.dag.disl.example.racer.runtime;

import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;



public class AdviceExecutor {
    private static final int NUMBER_OF_MAPS = 100000;
    private static MyWeakKeyIdentityHashMap[] ownerToFieldToState = new MyWeakKeyIdentityHashMap[NUMBER_OF_MAPS];
    private static final AdviceExecutor racer;
    static {
    	racer = new AdviceExecutor();
    	init();
    }
    public static AdviceExecutor getInstance() {
		return racer;
	}
    static void init() {
        for(int i = 0; i < ownerToFieldToState.length; i++) {
            ownerToFieldToState[i] = new MyWeakKeyIdentityHashMap(8);
        }
    }


    public void onFieldAccess(Object owner, String fieldID, String accessSite, Stack<Object> bag, boolean onRead) {
        try {
        	Thread t = Thread.currentThread(); 
            ConcurrentHashMap<String, FieldState> fieldToState;
            MyWeakKeyIdentityHashMap localOwnerToFieldToState = ownerToFieldToState[System.identityHashCode(owner) % NUMBER_OF_MAPS];
            synchronized(localOwnerToFieldToState) {
                if((fieldToState = (ConcurrentHashMap<String, FieldState>)localOwnerToFieldToState.get(owner)) == null) {
                    localOwnerToFieldToState.put(owner, fieldToState = new ConcurrentHashMap<String, FieldState>());
                }
            }
    
            String signature;
            FieldState currentState;
            if((currentState = fieldToState.get(signature = fieldID)) == null) {
                synchronized (fieldToState) {
                    if((currentState = fieldToState.get(signature = fieldID)) == null) {
                        fieldToState.put(signature, currentState = new FieldState(signature));
                    }
                }
            }
            if(onRead) {
                currentState.onRead(t, accessSite, bag);
            }
            else {
                currentState.onWrite(t, accessSite, bag);
            }
        } catch(Exception e) {
            System.out.println("[AdviceExecutor.onFieldAccess] ERROR WHILE PROCESSING THE BUFFER; ABORTING");
            e.printStackTrace();
            System.exit(-30);
        }
    }
	
}