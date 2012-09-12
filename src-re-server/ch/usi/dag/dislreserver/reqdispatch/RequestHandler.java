package ch.usi.dag.dislreserver.reqdispatch;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import ch.usi.dag.dislreserver.exception.DiSLREServerException;

public interface RequestHandler {

	void handle(DataInputStream is, DataOutputStream os, boolean debug)
			throws DiSLREServerException;
	
	// waits for processing completion in the case of separate threads
	void awaitProcessing();
	
	// invoked at exit
	void exit();
}
