package ch.usi.dag.dislserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicLong;

import ch.usi.dag.disl.DiSL;
import ch.usi.dag.disl.exception.DiSLException;
import ch.usi.dag.disl.util.Constants;

public class Worker extends Thread {

	private static final boolean debug = Boolean
			.getBoolean(DiSLServer.PROP_DEBUG);

	private static final String PROP_UNINSTR = "dislserver.uninstrumented";
	private static final String uninstrPath = 
			System.getProperty(PROP_UNINSTR, null);

	private static final String PROP_INSTR = "dislserver.instrumented";
	private static final String instrPath = 
			System.getProperty(PROP_INSTR, null);

	private final NetClassReader sc;
	private final DiSL disl;
	
	private final AtomicLong instrumentationTime = new AtomicLong();

	Worker(NetClassReader sc, DiSL disl) {
		this.sc = sc;
		this.disl = disl;
	}

	public void run() {

		try {

			instrumentationLoop();

			sc.close();
		}
		catch (Throwable e) {
			DiSLServer.reportError(e);
		}
		finally {
			DiSLServer.workerDone(instrumentationTime.get());
		}
	}

	private void instrumentationLoop() throws Exception {

		try {
		
		while (true) {

			ClassAsBytes cab = sc.readClassAsBytes();

			// communication closed by the client
			if (cab == null) {
				return;
			}

			byte[] instrClass;

			try {
				
				long startTime = System.nanoTime();
				
				instrClass = instrument(new String(cab.getName()),
						cab.getCode());
				
				instrumentationTime.addAndGet(System.nanoTime() - startTime);
			}
			catch (Exception e) {

				// instrumentation error
				// send the client a description of the server-side error

				String errToReport = e.getMessage();
				
				// during debug send the whole message
				if(debug) {
					StringWriter sw = new StringWriter();
					e.printStackTrace(new PrintWriter(sw));
					errToReport = sw.toString();
				}

				// error protocol:
				// class name contains the description of the server-side error
				// class code is an array of size zero
				String errMsg = "Instrumentation error for class "
						+ new String(cab.getName()) + ": " + errToReport;

				sc.sendClassAsBytes(new ClassAsBytes(errMsg.getBytes(),
						new byte[0]));

				throw e;
			}

			// default answer - no modification
			ClassAsBytes replyData = cab;
			
			// class was modified - send modified data
			if(instrClass != null) {
				replyData = new ClassAsBytes(cab.getName(), instrClass);
			}
			
			sc.sendClassAsBytes(replyData);
		}
		
		}
		catch (IOException e) {
			throw new DiSLServerException(e);
		}
	}
	
	private byte[] instrument(String className, byte[] origCode)
			throws DiSLServerException, DiSLException {
		
		// dump uninstrumented
		if (uninstrPath != null) {
			dump(className, origCode, uninstrPath);
		}

		// instrument
		byte[] instrCode = disl.instrument(origCode);

		// dump instrumented
		if (instrPath != null && instrCode != null) {
			dump(className, instrCode, instrPath);
		}

		return instrCode;
	}

	private void dump(String className, byte[] codeAsBytes, String path)
			throws DiSLServerException {
		
		try {
		
			// extract the class name and package name
			int i = className.lastIndexOf(Constants.PACKAGE_INTERN_DELIM);
			String onlyClassName = className.substring(i + 1);
			String packageName = className.substring(0, i + 1);
			
			// construct path to the class
			String pathWithPkg = path + File.pathSeparator + packageName;

			// create directories
			new File(pathWithPkg).mkdirs();

			// dump the class code
			FileOutputStream fo = new FileOutputStream(pathWithPkg
					+ onlyClassName + Constants.CLASS_EXT);
			fo.write(codeAsBytes);
			fo.close();
		}
		catch (FileNotFoundException e) {
			throw new DiSLServerException(e);
		}
		catch (IOException e) {
			throw new DiSLServerException(e);
		}
	}
}
