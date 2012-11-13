package ch.usi.dag.dislreserver.msg.objfree;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Set;

import ch.usi.dag.dislreserver.exception.DiSLREServerException;
import ch.usi.dag.dislreserver.msg.analyze.AnalysisResolver;
import ch.usi.dag.dislreserver.remoteanalysis.RemoteAnalysis;
import ch.usi.dag.dislreserver.reqdispatch.RequestHandler;
import ch.usi.dag.dislreserver.shadow.ShadowObject;
import ch.usi.dag.dislreserver.shadow.ShadowObjectTable;

public class ObjectFreeHandler implements RequestHandler {

	public void handle(DataInputStream is, DataOutputStream os, boolean debug)
			throws DiSLREServerException {

		try {
			long net_ref = is.readLong();
			ShadowObject obj = ShadowObjectTable.get(net_ref);

			Set<RemoteAnalysis> raSet = AnalysisResolver.getAllAnalyses();

			for (RemoteAnalysis ra : raSet) {
				ra.objectFree(obj);
			}

			ShadowObjectTable.freeShadowObject(net_ref, obj);

		} catch (IOException e) {
			throw new DiSLREServerException(e);
		}
	}

	public void awaitProcessing() {

	}

	public void exit() {

	}

}
