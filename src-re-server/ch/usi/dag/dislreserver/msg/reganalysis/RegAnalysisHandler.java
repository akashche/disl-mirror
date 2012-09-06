package ch.usi.dag.dislreserver.msg.reganalysis;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import ch.usi.dag.dislreserver.exception.DiSLREServerException;
import ch.usi.dag.dislreserver.msg.analyze.AnalysisResolver;
import ch.usi.dag.dislreserver.netreference.NetReference;
import ch.usi.dag.dislreserver.reqdispatch.RequestHandler;

public class RegAnalysisHandler implements RequestHandler {

	public void handle(DataInputStream is, DataOutputStream os, boolean debug)
			throws DiSLREServerException {
		
		try {

			short methodId = is.readShort();
			NetReference methodStringNR = new NetReference(is.readLong());
			
			// register method
			AnalysisResolver.registerMethodId(methodId, 
					methodStringNR.getObjectId());
			
		} catch (IOException e) {
			throw new DiSLREServerException(e);
		}
	}

	public void exit() {
		
	}

}
