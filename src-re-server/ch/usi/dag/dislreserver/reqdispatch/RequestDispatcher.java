package ch.usi.dag.dislreserver.reqdispatch;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.HashMap;
import java.util.Map;

import ch.usi.dag.dislreserver.exception.DiSLREServerException;
import ch.usi.dag.dislreserver.exception.DiSLREServerFatalException;
import ch.usi.dag.dislreserver.msg.analyze.AnalysisHandler;
import ch.usi.dag.dislreserver.msg.classinfo.ClassInfoHandler;
import ch.usi.dag.dislreserver.msg.close.CloseHandler;
import ch.usi.dag.dislreserver.msg.newclass.NewClassHandler;
import ch.usi.dag.dislreserver.msg.newstring.NewStringHandler;
import ch.usi.dag.dislreserver.msg.objfree.ObjectFreeHandler;
import ch.usi.dag.dislreserver.msg.reganalysis.RegAnalysisHandler;

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
		// new class
		requestMap.put(3, new NewClassHandler());
		// class info
		requestMap.put(4, new ClassInfoHandler());
		// new string
		requestMap.put(5, new NewStringHandler());
		// new string
		requestMap.put(6, new RegAnalysisHandler());
	}
	
	public static boolean dispatch(int requestNo, DataInputStream is,
			DataOutputStream os, boolean debug) throws DiSLREServerException {

		// request handler
		RequestHandler rh = requestMap.get(requestNo);
		
		if(rh == null) {
			
			throw new DiSLREServerFatalException("Message type (" + requestNo +
					") not supported");
		}
		
		if(debug) {
			System.out.println("Dispatching " + rh.getClass().getName());
		}
		
		// process request
		rh.handle(is, os, debug);
		
		// if close handler is there then exit
		if(rh instanceof CloseHandler) {
			
			// call exit on all handlers
			for(RequestHandler reqhl : requestMap.values()) {
				reqhl.exit();
			}
			
			// return exit confirmation
			return true;
		}
		
		return false;
	}

}
