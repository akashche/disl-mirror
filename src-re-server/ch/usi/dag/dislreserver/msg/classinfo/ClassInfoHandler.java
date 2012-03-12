package ch.usi.dag.dislreserver.msg.classinfo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import ch.usi.dag.dislreserver.exception.DiSLREServerException;
import ch.usi.dag.dislreserver.netreference.NetReference;
import ch.usi.dag.dislreserver.reflectiveinfo.ClassInfoResolver;
import ch.usi.dag.dislreserver.reqdispatch.RequestHandler;

public class ClassInfoHandler implements RequestHandler {

	public void handle(DataInputStream is, DataOutputStream os, boolean debug)
			throws DiSLREServerException {

		try {
			
			int classId = is.readInt();
			String classSignature = is.readUTF();
			String classGenericStr = is.readUTF();
			NetReference classLoaderNR = new NetReference(is.readLong());
			int superClassId = is.readInt();
			
			ClassInfoResolver.createHierarchy(classSignature, classGenericStr, classLoaderNR, classId, superClassId);
		}
		catch (IOException e) {
			throw new DiSLREServerException(e);
		}
	}

}
