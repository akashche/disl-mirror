package ch.usi.dag.dislreserver.msg.analyze;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import ch.usi.dag.dislreserver.exception.DiSLREServerFatalException;

// NOTE: Task is list of analysis methods from the same thread.
// Dispatches new task when new one is added or when the running one is
// completed so there is at most one task running.
public class AnalysisDispatcher {

	ConcurrentMap<Long, AnalysisThreadTasks> threadMap =
			new ConcurrentHashMap<Long, AnalysisThreadTasks>();
	
	ExecutorService execSrvc = Executors.newCachedThreadPool();
	
	Object waitObject = new Object();
	
	// NOTE: access to this object should be synchronized
	// Holds all unprocessed tasks for some thread. It also dispatches tasks
	// as necessary.
	private static class AnalysisThreadTasks {

		ExecutorService execSrvc;
		private boolean inProgress = false;
		private Queue<AnalysisTask> tasks = new LinkedList<AnalysisTask>();

		public AnalysisThreadTasks(ExecutorService execSrvc) {
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
			
			// notify all waiting threads
			this.notifyAll();
			
			dispatchTask();
		}
		
		// Returns when no task is running and no task can be scheduled 
		public synchronized void awaitProcessing() throws InterruptedException {

			while(true) {
			
				if(inProgress == false && tasks.isEmpty()) {
					return;
				}
				
				this.wait();
			}
		}
	}
	
	private static class AnalysisTask implements Runnable {

		// holds reference to the recored where it was stored for next task
		// dispatch
		AnalysisThreadTasks att;
		List<AnalysisInvocation> invocations;
		
		public AnalysisTask(AnalysisThreadTasks att,
				List<AnalysisInvocation> invocations) {
			super();
			this.att = att;
			this.invocations = invocations;
		}

		public void run() {
			
			// invoke all methods in this task
			for(AnalysisInvocation ai : invocations) {
				ai.invoke();
			}
			
			// this task is completed - dispatch new one if possible
			att.taskCompleted();
		}
		
	}
	
	public void addTask(long orderingID,
			List<AnalysisInvocation> invocations) {
		
		// add task to the dispatch record
		
		AnalysisThreadTasks att = threadMap.get(orderingID);
		
		if(att == null) {
			
			// create new dispatch record
			// in the case of concurrent allocations putIfAbsent guarantees only
			// one proper value
			att = new AnalysisThreadTasks(execSrvc);
			AnalysisThreadTasks old = threadMap.putIfAbsent(orderingID, att);
			
			// replace with proper value
			if(old != null) {
				att = old;
			}
		}
		
		// create new task to add
		AnalysisTask at = new AnalysisTask(att, invocations);
		
		// add task - dispatch, if it is possible
		att.addTask(at);
	}

	public void awaitProcessing() {
		
		try {
			
			// no new thread will be added, no new task will be added
			//  - only main thread is adding tasks and it will wait here :)
			for(AnalysisThreadTasks att : threadMap.values()) {
				att.awaitProcessing();
			}
		}
		catch (InterruptedException e) {
			throw new DiSLREServerFatalException("Main thread interupted while"
					+ " waiting on analysis to complete", e);
		}
	}
	
	public void exit() {
		
		execSrvc.shutdown();
		
		try {
			// wait for termination in the loop
			while(! execSrvc.awaitTermination(60, TimeUnit.SECONDS));
		}
		catch (InterruptedException e) {
			throw new DiSLREServerFatalException("Main thread interupted while"
					+ " waiting on analysis to complete", e);
		}
	}
}
