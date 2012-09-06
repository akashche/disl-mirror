package ch.usi.dag.dislreserver.msg.analyze;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ch.usi.dag.dislreserver.netreference.NetReference;

// NOTE: Task is list of analysis methods from the same thread.
// Dispatches new task when new one is added or when the running one is
// completed so there is at most one task running.
public class AnalysisDispatcher {

	ConcurrentMap<NetReference, AnalysisDispRec> threadMap =
			new ConcurrentHashMap<NetReference, AnalysisDispRec>();
	
	ExecutorService execSrvc = Executors.newCachedThreadPool();
	
	// NOTE: access to this object should be synchronized
	// Holds all unprocessed tasks for some thread. It also dispatches tasks
	// as necessary.
	private static class AnalysisDispRec {

		ExecutorService execSrvc;
		private boolean inProgress = false;
		private Queue<AnalysisTask> tasks = new LinkedList<AnalysisTask>();

		public AnalysisDispRec(ExecutorService execSrvc) {
			this.execSrvc = execSrvc;
		}

		// Dispatches (adds to the thread pool) new task from dispatch record if
		// there is one and can be dispatched.
		private void dispatchTask() {
			
			// nothing is currently executed and something can be :)
			if(inProgress == false && ! tasks.isEmpty()) {
				
				inProgress = true;
				
				// exec new task using thread pool
				// hopefully we can do it from multiple thread see:
				// http://stackoverflow.com/questions/1702386/is-threadpoolexecutor-thread-safe
				execSrvc.submit(tasks.poll());
			}
		}
		
		// Adds new task to the record
		public synchronized void addTask(AnalysisTask newTaskToAdd) {

			tasks.add(newTaskToAdd);
			
			dispatchTask();
		}

		// Announces the completion of previous task
		public synchronized void taskCompleted() {
			
			inProgress = false;
			
			dispatchTask();
		}
	}
	
	private static class AnalysisTask implements Runnable {

		// holds reference to the recored where it was stored for next task
		// dispatch
		AnalysisDispRec adRec;
		List<AnalysisInvocation> invocations;
		
		public AnalysisTask(AnalysisDispRec adRec,
				List<AnalysisInvocation> invocations) {
			super();
			this.adRec = adRec;
			this.invocations = invocations;
		}

		public void run() {
			
			// invoke all methods in this task
			for(AnalysisInvocation ai : invocations) {
				ai.invoke();
			}
			
			// this task is completed - dispatch new one if possible
			adRec.taskCompleted();
		}
		
	}
	
	public void addTask(NetReference threadNR,
			List<AnalysisInvocation> invocations) {
		
		// add task to the dispatch record
		
		AnalysisDispRec adRec = threadMap.get(threadNR);
		
		if(adRec == null) {
			
			// create new dispatch record
			adRec = new AnalysisDispRec(execSrvc);
			threadMap.put(threadNR, adRec);
		}
		
		// create new task to add
		AnalysisTask at = new AnalysisTask(adRec, invocations);
		
		// add task - dispatch if if possible
		adRec.addTask(at);
	}

	public void exit() {
		execSrvc.shutdown();
	}
}
