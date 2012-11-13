package ch.usi.dag.dislreserver.msg.threadinfo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import ch.usi.dag.dislreserver.exception.DiSLREServerException;
import ch.usi.dag.dislreserver.reqdispatch.RequestHandler;
import ch.usi.dag.dislreserver.shadow.ShadowObjectTable;
import ch.usi.dag.dislreserver.shadow.ShadowThread;

public class ThreadInfoHandler implements RequestHandler {

	public void handle(DataInputStream is, DataOutputStream os, boolean debug)
			throws DiSLREServerException {

		try {

			long net_ref = is.readLong();
			String name = is.readUTF();
			boolean isDaemon = is.readBoolean();

			ShadowThread sThread = new ShadowThread(net_ref, name, isDaemon);
			ShadowObjectTable.register(sThread);
		} catch (IOException e) {
			throw new DiSLREServerException(e);
		}
	}

	public void awaitProcessing() {

	}

	public void exit() {

	}
}
