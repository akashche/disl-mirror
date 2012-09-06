package ch.usi.dag.dislreserver.msg.objfree;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Set;

import ch.usi.dag.dislreserver.exception.DiSLREServerException;
import ch.usi.dag.dislreserver.msg.analyze.AnalysisResolver;
import ch.usi.dag.dislreserver.netreference.NetReference;
import ch.usi.dag.dislreserver.remoteanalysis.RemoteAnalysis;
import ch.usi.dag.dislreserver.reqdispatch.RequestHandler;

public class ObjectFreeHandler implements RequestHandler {

	public void handle(DataInputStream is, DataOutputStream os, boolean debug)
			throws DiSLREServerException {
		
		try {
			NetReference netRef = new NetReference(is.readLong());
				
			Set<RemoteAnalysis> raSet = AnalysisResolver.getAllAnalyses();
			
			for(RemoteAnalysis ra : raSet) {
				ra.objectFree(netRef);
			}
			
			// TODO ! re - free for special objects
		
		} catch (IOException e) {
			throw new DiSLREServerException(e);
		}
	}

	public void exit() {
		
	}

}
