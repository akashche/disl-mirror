package ch.usi.dag.dislreserver.msg.close;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Set;

import ch.usi.dag.dislreserver.msg.analyze.AnalysisResolver;
import ch.usi.dag.dislreserver.remoteanalysis.RemoteAnalysis;
import ch.usi.dag.dislreserver.reqdispatch.RequestHandler;

public class CloseHandler implements RequestHandler {

	public void handle(DataInputStream is, DataOutputStream os, boolean debug) {
		
		// invoke all atExit on registered analyses
		Set<RemoteAnalysis> raSet = AnalysisResolver.getAllAnalyses();
		
		for(RemoteAnalysis ra : raSet) {
			ra.atExit();
		}
	}

}
