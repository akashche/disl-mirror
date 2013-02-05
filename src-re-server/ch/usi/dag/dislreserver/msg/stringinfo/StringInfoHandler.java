package ch.usi.dag.dislreserver.msg.stringinfo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import ch.usi.dag.dislreserver.exception.DiSLREServerException;
import ch.usi.dag.dislreserver.reqdispatch.RequestHandler;
import ch.usi.dag.dislreserver.shadow.ShadowObjectTable;
import ch.usi.dag.dislreserver.shadow.ShadowString;

public class StringInfoHandler implements RequestHandler {

	public void handle(DataInputStream is, DataOutputStream os, boolean debug)
			throws DiSLREServerException {

		try {

			long net_ref = is.readLong();
			String str = is.readUTF();

			ShadowString sString = new ShadowString(net_ref, str);
			ShadowObjectTable.register(sString, debug);
		} catch (IOException e) {
			throw new DiSLREServerException(e);
		}
	}

	public void awaitProcessing() {

	}

	public void exit() {

	}

}
