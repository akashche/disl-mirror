package ch.usi.dag.dislreserver.msg.reganalysis;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import ch.usi.dag.dislreserver.exception.DiSLREServerException;
import ch.usi.dag.dislreserver.msg.analyze.AnalysisResolver;
import ch.usi.dag.dislreserver.netreference.NetReference;
import ch.usi.dag.dislreserver.reqdispatch.RequestHandler;
import ch.usi.dag.dislreserver.stringcache.StringCache;


public final class RegAnalysisHandler implements RequestHandler {

	public void handle (
		final DataInputStream is, final DataOutputStream os, final boolean debug
	) throws DiSLREServerException {
		try {
			final short methodId = is.readShort ();
			NetReference methodStringNR = new NetReference (is.readLong ());

			// register method
			AnalysisResolver.registerMethodId (
				methodId, methodStringNR.getObjectId ()
			);

			if (debug) {
				System.out.printf (
					"DiSL-RE: registered %s as analysis method %d\n",
					StringCache.resolve (methodStringNR.getObjectId ()), methodId
				);
			}

		} catch (final IOException ioe) {
			throw new DiSLREServerException (ioe);
		}
	}


	//

	public void awaitProcessing () {

	}


	public void exit () {

	}

}
