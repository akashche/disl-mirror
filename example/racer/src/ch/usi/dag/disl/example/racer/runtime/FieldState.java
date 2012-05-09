package ch.usi.dag.disl.example.racer.runtime;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Stack;







public final class FieldState {
    public static final int VIRGIN = 0;
    public static final int EXCLUSIVE = 1;
    public static final int MODIFIED = 2;
    public static final int SHARED = 3;
    public static final int SHARED_MODIFIED = 4;
    public static final int REPORTED_RACE = 5;

    private static Set<String> reportedRaces;

    private final String fieldSignature;

    private LinkedList<String> readList = new LinkedList<String>();
    private LinkedList<String> writeList = new LinkedList<String>();

    private MyList<Object> locksList;

	private int currentState;
	private Thread t;
	private boolean virgin;

    static {
        init();
    }

    public FieldState(String fieldSignature) {
		this.fieldSignature = fieldSignature;
		currentState = VIRGIN;
		virgin = true;
	}

    public static void init() {
        reportedRaces = new HashSet<String>();
    }

	public synchronized void onRead(Thread t, String fieldID, Stack<Object> bag) {
	    boolean reportRace = false;
	    switch(currentState) {
    	    case VIRGIN:
    	        currentState = EXCLUSIVE;
    	        this.t = t;
    	        readList.add(fieldID);
    	        updateLocks(bag);
    	        return;
    	    case EXCLUSIVE:
    	        if(this.t != t) {
    	            currentState = SHARED;
    	            readList.add(fieldID);
    	        }
    	        updateLocks(bag);
    	        return;
    	    case MODIFIED:
    	        if(this.t != t) {
    	            readList.add(fieldID);
    	            updateLocks(bag);
    	            if(reportRace = needToReportRace()) {
    	                currentState = REPORTED_RACE;
    	                break;
    	            }
    	            else {
    	                currentState = SHARED_MODIFIED;
    	                return;
    	            }
    	        }
    	        else {
    	            updateLocks(bag);
    	        }
    	        return;
    	    case SHARED:
    	        updateLocks(bag);
    	        return;
    	    case SHARED_MODIFIED:
    	        updateLocks(bag);
    	        if(reportRace = needToReportRace()) {
    	            currentState = REPORTED_RACE;
    	            break;
    	        }
    	        return;
    	    case REPORTED_RACE:
    	        return;
	    }
	    if(reportRace) {
	        reportRace();
	    }
	}

	public synchronized void onWrite(Thread t, String fieldID, Stack<Object> bag) {
	    boolean reportRace = false;
	    switch(currentState) {
	        case VIRGIN:
	            currentState = MODIFIED;
	            this.t = t;
	            writeList.add(fieldID);
	            updateLocks(bag);
	            return;
	        case EXCLUSIVE:
	            if(this.t == t) {
	                currentState = MODIFIED;
	                writeList.add(fieldID);
	                updateLocks(bag);
	                return;
	            }
	            else {
	                writeList.add(fieldID);
	                updateLocks(bag);
	                if(reportRace = needToReportRace()) {
	                    currentState = REPORTED_RACE;
	                    break;
	                }
	                else {
	                    currentState = SHARED_MODIFIED;
	                }
	            }
	            return;
	        case MODIFIED:
	            if(this.t != t) {
	                writeList.add(fieldID);
	                updateLocks(bag);
	                if(reportRace = needToReportRace()) {
	                    currentState = REPORTED_RACE;
	                    break;
	                }
	                else {
	                    currentState = SHARED_MODIFIED;
	                    return;
	                }
	            }
	            else {
	                updateLocks(bag);
	            }
	            return;
	        case SHARED:
	            writeList.add(fieldID);
	            updateLocks(bag);
	            if(reportRace = needToReportRace()) {
	                currentState = REPORTED_RACE;
	                break;
	            }
	            else {
	                currentState = SHARED_MODIFIED; 
	            }
	            return;
	        case SHARED_MODIFIED:
	            updateLocks(bag);
	            if(reportRace = needToReportRace()) {
	                currentState = REPORTED_RACE;
	                break;
	            }
	            return;
	        case REPORTED_RACE:
	            return;
	    }
	    if(reportRace) {
	        reportRace();
	    }
	}

	 private void updateLocks(Stack<Object> stack) {
	        if(virgin) {
	            virgin = false;
	            int index;
	            if((index = stack.size()) != 0) {
	                locksList = new MyList<Object>(stack.toArray(), index);
	            }
	        }
	        else {
	            if(locksList != null) {
	                locksList.retainAll(stack.toArray(), stack.size());
	            }
	        }
	    }
	 

	private boolean needToReportRace() {
		if((locksList == null) || (locksList.isEmpty())) {
	        synchronized(reportedRaces) {
	            return reportedRaces.add(fieldSignature);
	        }
	    }
	    return false;
	}

	private void reportRace() {
	    System.err.print("==========================\nRace condition found!\nUnprotected access to field: " + fieldSignature);
	    String accessHistory = new String();
	    while(!readList.isEmpty()) {
	        accessHistory += "\nREAD: " + readList.removeFirst();
	    }
	    while(!writeList.isEmpty()) {
	        accessHistory += "\nWRITE: " + writeList.removeFirst();
	    }
	    System.err.println(accessHistory + "\n==========================\n");
	}
}
