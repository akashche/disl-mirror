package ch.usi.dag.dislreserver.msg.newclass;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import ch.usi.dag.dislreserver.exception.DiSLREServerException;
import ch.usi.dag.dislreserver.netreference.NetReference;
import ch.usi.dag.dislreserver.reflectiveinfo.ClassInfoResolver;
import ch.usi.dag.dislreserver.reqdispatch.RequestHandler;

public class NewClassHandler implements RequestHandler {

	public void handle(DataInputStream is, DataOutputStream os, boolean debug)
			throws DiSLREServerException {
		
		try {
		
			String className = is.readUTF();
			NetReference classLoaderNR = new NetReference(is.readLong());
			int classCodeLength = is.readInt();
			byte[] classCode = new byte[classCodeLength];
			is.readFully(classCode);
			
			ClassInfoResolver.addNewClass(className, classLoaderNR, classCode);
		}
		catch (IOException e) {
			throw new DiSLREServerException(e);
		}
	}

	public void awaitProcessing() {
		
	}
	
	public void exit() {
		
	}

}
