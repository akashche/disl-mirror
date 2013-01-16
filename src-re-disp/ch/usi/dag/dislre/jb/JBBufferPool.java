package ch.usi.dag.dislre.jb;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

// TODO ! JB - this class is not unique
// a) instance from some jcl class
// b) pretend that we are jcl class

public class JBBufferPool {

	// TODO ! JB - better exception handling
	
	public static final int NUMBER_OF_FETCHING_THREADS = 4;
    private static final int BUFFERS = NUMBER_OF_FETCHING_THREADS * 10;

    private static final List<JBBuffer> allBuffers;
    private static final BlockingQueue<JBBuffer> fullBuffers;
    private static final BlockingQueue<JBBuffer> emptyBuffers;

    static {
        
    	try {
    		
    		allBuffers = new ArrayList<JBBuffer>(BUFFERS);
        	fullBuffers = new ArrayBlockingQueue<JBBuffer>(BUFFERS);
            emptyBuffers = new ArrayBlockingQueue<JBBuffer>(BUFFERS);

            // fill desired queues
            for(int i = 0; i < BUFFERS; i++) {
            	
            	allBuffers.add(new JBBuffer());
                emptyBuffers.put(new JBBuffer());
            }
            
            register(allBuffers, fullBuffers, emptyBuffers);

        } catch(Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static JBBuffer getEmpty() {
    	
    	while(true) {
            
    		try {
    			JBBuffer buffer = emptyBuffers.take();
                buffer.init();
                return buffer;
    		}
    		catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void putFull(JBBuffer buffer) {
        
    	boolean done = false;
    	while(!done) {
            
    		try {
            
    			fullBuffers.put(buffer);
                done = true;
                
            }
    		catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // called by the native code
    private static JBBuffer getFull() {
    	
    	// get buffer
    	while(true) {
            
    		try {
    			return fullBuffers.take();
                
            }
    		catch(InterruptedException e) {
            	e.printStackTrace();
            }
        }
    }

    // called (also) by the native code
    private static void putEmpty(JBBuffer buffer) {
        
    	boolean done = false;
    	while(! done) {
            
    		try {
                
    			buffer.reset();
            	emptyBuffers.put(buffer);
                done = true;
                
            }
    		catch(InterruptedException e) {
            	e.printStackTrace();
            }
        }
    }

    private static native void register(
    		List<JBBuffer> allBuffers,
    		BlockingQueue<JBBuffer> fullBuffers,
            BlockingQueue<JBBuffer> emptyBuffers);
}
