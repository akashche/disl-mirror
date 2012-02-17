package ch.usi.dag.dislreserver.msg.analyze;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

import ch.usi.dag.dislreserver.DiSLREServerException;
import ch.usi.dag.dislreserver.msg.analyze.AnalysisResolver.AnalysisMethodHolder;
import ch.usi.dag.dislreserver.objectid.InvalidClass;
import ch.usi.dag.dislreserver.objectid.ObjectId;
import ch.usi.dag.dislreserver.reqdispatch.RequestHandler;

public class AnalysisHandler implements RequestHandler {

	public void handle(DataInputStream is, DataOutputStream os, boolean debug)
			throws DiSLREServerException {

		try {

			// *** retrieve method ***
			
			// read method id from network
			int analysisMethodId = is.readInt();
			
			// retrieve method
			AnalysisMethodHolder amh = AnalysisResolver.getMethod(analysisMethodId);
			
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
				
				Object argValue = readType(is, argClass);
				
				if(argValue == null) {
					throw new DiSLREServerException(
							"Unsupported data type "
							+ argClass.toString()
							+ " in analysis method "
							+ analysisMethod.getDeclaringClass().toString()
							+ "."
							+ analysisMethod.toString());
				}
				
				args.add(argValue);
			}
			
			// *** invoke method ***

			try {
				// TODO re ! support static methods
				analysisMethod.invoke(amh.getAnalysisInstance(), args.toArray());
			}
			catch(InvocationTargetException e) {
				
				// report analysis error
				
				Throwable cause = e.getCause();
				
				System.err.println("DiSL-RE analysis exception: "
						+ cause.getMessage());
				
				cause.printStackTrace();
			}
			catch(Exception e) {
				
				throw new DiSLREServerException(e);
			}
			
		}
		catch (IOException e) {
			throw new DiSLREServerException(e);
		}
		catch (IllegalArgumentException e) {
			throw new DiSLREServerException(e);
		}
	}

	private Object readType(DataInputStream is, Class<?> argClass)
			throws IOException {
		
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
			return is.readUTF();
		}

		// read id only
		if(argClass.equals(Object.class)) {
			return new ObjectId(is.readLong());
		}
		
		// return "invalid" class object
		if(argClass.equals(Class.class)) {
			return InvalidClass.class;
		}
		
		return null;
	}

}
