package ch.usi.dag.dislreserver.msg.newstring;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import ch.usi.dag.dislreserver.exception.DiSLREServerException;
import ch.usi.dag.dislreserver.netreference.NetReference;
import ch.usi.dag.dislreserver.reqdispatch.RequestHandler;
import ch.usi.dag.dislreserver.stringcache.StringCache;

public class NewStringHandler implements RequestHandler {

	public void handle(DataInputStream is, DataOutputStream os, boolean debug)
			throws DiSLREServerException {
		
		try {
			
			NetReference stringNR = new NetReference(is.readLong());
			String str = is.readUTF();
			
			StringCache.register(stringNR.getObjectId(), str);
		}
		catch (IOException e) {
			throw new DiSLREServerException(e);
		}
	}

}
