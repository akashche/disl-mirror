package ch.usi.dag.dislre.jb;

public class TagBuffer {

	private Object[] objArray;
    private int[]    posArray;
    private int count;
	
    public TagBuffer(int size) {
		
		this.objArray = new Object[size];;
		this.posArray = new int[size];
		this.count = 0;
	}

    public void add(Object obj, int pos) {

    	// TODO ! JB - throws an exception if too many added, ok for now
    	
    	objArray[count] = obj;
    	posArray[count] = pos;
    	++count;
    }
    
	public void reset() {
		
    	//set buffered object references to null
        for(int i = 0; i < count; ++i) {
        	objArray[i] = null;
        }
        
        count = 0;
	}
    
	public Object[] getObjArray() {
		return objArray;
	}

	public int[] getPosArray() {
		return posArray;
	}
	
	public int size() {
		return count;
	}
}
