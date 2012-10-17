package ch.usi.dag.dislreserver.msg.analyze;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import ch.usi.dag.dislreserver.exception.DiSLREServerException;
import ch.usi.dag.dislreserver.msg.analyze.AnalysisResolver.AnalysisMethodHolder;
import ch.usi.dag.dislreserver.netreference.NetReference;
import ch.usi.dag.dislreserver.reflectiveinfo.ClassInfo;
import ch.usi.dag.dislreserver.reflectiveinfo.ClassInfoResolver;
import ch.usi.dag.dislreserver.reflectiveinfo.InvalidClass;
import ch.usi.dag.dislreserver.reqdispatch.RequestHandler;
import ch.usi.dag.dislreserver.stringcache.StringCache;

public class AnalysisHandler implements RequestHandler {

	private AnalysisDispatcher dispatcher = new AnalysisDispatcher();
	
	public void handle(DataInputStream is, DataOutputStream os, boolean debug)
			throws DiSLREServerException {
	
		try {
			
			// get net reference for the thread
			long orderingID = is.readLong();
			
			// read  and create method invocations
			int numberOfMethods = is.readInt();
			
			List<AnalysisInvocation> invocations
				= new LinkedList<AnalysisInvocation>();
			
			for(int i = 0; i < numberOfMethods; ++i) {
				invocations.add(createInvocation(is, debug));
			}
			
			dispatcher.addTask(orderingID, invocations);
		}
		catch (IOException e) {
			throw new DiSLREServerException(e);
		}
	}

	public AnalysisInvocation createInvocation(DataInputStream is, boolean debug)
			throws DiSLREServerException {

		try {

			// *** retrieve method ***
			
			// read method id from network and retrieve method
			AnalysisMethodHolder amh = 
					AnalysisResolver.getMethod(is.readShort());
			
			if(debug) {
				System.out.println("Processing analysis "
						+ amh.getAnalysisInstance().getClass().getName()
						+ "."
						+ amh.getAnalysisMethod().getName()
						+ "()");
			}
						
			// *** retrieve method argument values ***
			
			Method analysisMethod = amh.getAnalysisMethod();
			
			// prepare argument value list
			List<Object> args = new LinkedList<Object>();
			
			// read data according to argument types
			for(Class<?> argClass : analysisMethod.getParameterTypes()) {
				
				Object argValue = readType(is, argClass, analysisMethod);
				
				args.add(argValue);
			}
			
			// *** create analysis invocation ***

			return new AnalysisInvocation(analysisMethod,
					amh.getAnalysisInstance(), args);
			
		}
		catch (IOException e) {
			throw new DiSLREServerException(e);
		}
		catch (IllegalArgumentException e) {
			throw new DiSLREServerException(e);
		}
	}

	private Object readType(DataInputStream is, Class<?> argClass,
			Method analysisMethod) throws IOException, DiSLREServerException {
		
		if(argClass.equals(boolean.class)) {
			return is.readBoolean();
		}
		
		if(argClass.equals(char.class)) {
			return is.readChar();
		}
		
		if(argClass.equals(byte.class)) {
			return is.readByte();
		}
		
		if(argClass.equals(short.class)) {
			return is.readShort();
		}
		
		if(argClass.equals(int.class)) {
			return is.readInt();
		}
		
		if(argClass.equals(long.class)) {
			return is.readLong();
		}
		
		if(argClass.equals(float.class)) {
			return is.readFloat();
		}
		
		if(argClass.equals(double.class)) {
			return is.readDouble();
		}
		
		if(argClass.equals(String.class)) {

			long netRefNum = is.readLong();
			
			// null handling
			if(netRefNum == 0) {
				return null;
			}
			
			// read string net reference
			NetReference stringNR = new NetReference(netRefNum);
			// resolve string from cache 
			return StringCache.resolve(stringNR.getObjectId());
		}

		// read id only
		// covers Object and NetReference classes
		if(argClass.isAssignableFrom(NetReference.class)) {
			
			long netRefNum = is.readLong();
			
			// null handling
			if(netRefNum == 0) {
				return null;
			}
			
			return new NetReference(netRefNum);
		}
		
		// return ClassInfo object
		if(argClass.equals(ClassInfo.class)) {
			
			int classNetRefNum = is.readInt();
			
			// null handling
			if(classNetRefNum == 0) {
				return null;
			}
			
			return ClassInfoResolver.getClass(classNetRefNum);
		}
		
		// return "invalid" class object
		if(argClass.equals(Class.class)) {
			return InvalidClass.class;
		}
		
		throw new DiSLREServerException(
				"Unsupported data type "
				+ argClass.toString()
				+ " in analysis method "
				+ analysisMethod.getDeclaringClass().toString()
				+ "."
				+ analysisMethod.toString());
	}

	public void awaitProcessing() {
		dispatcher.awaitProcessing();
	}
	
	public void exit() {
		dispatcher.exit();
	}

}
