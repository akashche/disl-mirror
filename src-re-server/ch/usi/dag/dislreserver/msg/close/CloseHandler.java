package ch.usi.dag.dislreserver.msg.close;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Set;

import ch.usi.dag.dislreserver.msg.analyze.AnalysisResolver;
import ch.usi.dag.dislreserver.remoteanalysis.RemoteAnalysis;
import ch.usi.dag.dislreserver.reqdispatch.RequestDispatcher;
import ch.usi.dag.dislreserver.reqdispatch.RequestHandler;

public class CloseHandler implements RequestHandler {

	public void handle(DataInputStream is, DataOutputStream os, boolean debug) {

		// call exit on all handlers - waits for all uncompleted actions
		for (RequestHandler reqhl : RequestDispatcher.getRequestmap().values()) {
			reqhl.exit();
		}
		
		// invoke all atExit on registered analyses
		Set<RemoteAnalysis> raSet = AnalysisResolver.getAllAnalyses();
		
		for(RemoteAnalysis ra : raSet) {
			ra.atExit();
		}
	}

	public void awaitProcessing() {
		
	}
	
	public void exit() {
		
	}
}
