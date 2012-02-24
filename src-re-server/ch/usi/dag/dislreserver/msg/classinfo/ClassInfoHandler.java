package ch.usi.dag.dislreserver.msg.classinfo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import ch.usi.dag.dislreserver.exception.DiSLREServerException;
import ch.usi.dag.dislreserver.netreference.NetReference;
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
			
			// TODO re ! process class info
			// TODO re ! remove
			/*
			System.out.println("* Class info *");
			System.out.println("Class id: " + classId);
			System.out.println("Class sig: " + classSignature);
			System.out.println("Class gen: " + classGenericStr);
			System.out.println("Class loader id: " + classLoaderNR.getObjectId());
			System.out.println("Super class id: " + superClassId);
			*/
		}
		catch (IOException e) {
			throw new DiSLREServerException(e);
		}
	}

}
