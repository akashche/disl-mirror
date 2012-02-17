package ch.usi.dag.dislreserver.reqdispatch;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.HashMap;
import java.util.Map;

import ch.usi.dag.dislreserver.exception.DiSLREServerException;
import ch.usi.dag.dislreserver.exception.DiSLREServerFatalException;
import ch.usi.dag.dislreserver.msg.analyze.AnalysisHandler;
import ch.usi.dag.dislreserver.msg.close.CloseHandler;
import ch.usi.dag.dislreserver.msg.objfree.ObjectFreeHandler;

public class RequestDispatcher {

	private static final Map<Integer, RequestHandler> requestMap;
	
	static {
		requestMap = new HashMap<Integer, RequestHandler>();
		
		// Messages - should be in sync with c agent
		
		// close
		requestMap.put(0, new CloseHandler());
		// analyze
		requestMap.put(1, new AnalysisHandler());
		// object free
		requestMap.put(2, new ObjectFreeHandler());
	}
	
	public static boolean dispatch(int requestNo, DataInputStream is,
			DataOutputStream os, boolean debug) throws DiSLREServerException {

		// request handler
		RequestHandler rh = requestMap.get(requestNo);
		
		if(debug) {
			System.out.println("Dispatching " + rh.getClass().getName());
		}

		if(rh == null) {
			
			throw new DiSLREServerFatalException("Message type (" + requestNo +
					") not supported");
		}
		
		// process request
		rh.handle(is, os, debug);
		
		// if close handler is there then exit
		return rh instanceof CloseHandler;
	}

}
