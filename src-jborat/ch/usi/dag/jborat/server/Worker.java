package ch.usi.dag.jborat.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import ch.usi.dag.disl.DiSL;
import ch.usi.dag.disl.exception.DiSLException;
import ch.usi.dag.disl.scope.WildCard;

import com.sun.xml.internal.ws.org.objectweb.asm.Type;

public class Worker extends Thread {

	private static final boolean debug = Boolean
			.getBoolean(InstrumentationServer.PROP_DEBUG);

	private static final String PROP_EXCLIST = "jborat.exclusionList";
	private static final String excListPath = 
			System.getProperty(PROP_EXCLIST, null);

	private static final String PROP_UNINSTR = "jborat.uninstrumented";
	private static final String uninstrPath = 
			System.getProperty(PROP_UNINSTR, null);

	private static final String PROP_INSTR = "jborat.instrumented";
	private static final String instrPath = 
			System.getProperty(PROP_INSTR, null);

	private static Set<String> exclusionList;

	private final NetClassReader sc;
	private final DiSL disl;

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
			InstrumentationServer.reportError(e);
		}
		finally {
			InstrumentationServer.workerDone();
		}
	}

	private boolean filterClass(String className) throws JboratException {

		// race condition can be here (invocation from multiple threads)
		// but this is not serious problem
		if(exclusionList == null) {
			exclusionList = readExlusionList();
		}
		
		// check the exclusion list for possible matches
		for(String pattern : exclusionList) {
			
			if(WildCard.match(className, pattern)) {
				return false;
			}
		}

		if (debug) {
			System.out.println("Excluding class: " + className);
		}

		if (className.equals(Type.getInternalName(Thread.class))) {
			throw new JboratException(Thread.class.getName()
					+ " cannot be excluded in exclusion list");
		}

		return true;
	}

	private Set<String> readExlusionList() throws JboratException {

		final String COMMENT_START = "#";
		
		try {
		
			Set<String> exclSet = new HashSet<String>();

			// read exclusion list line by line
			Scanner scanner = new Scanner(new FileInputStream(excListPath));
			while (scanner.hasNextLine()) {
				
				String line = scanner.nextLine();
				
				if(! line.startsWith(COMMENT_START)) {
					exclSet.add(internClassName(line));
				}
			}

			scanner.close();

			// TODO jb ! add classes from agent and instrumentation jar
			
			return exclSet;
		
		} catch(FileNotFoundException e) {
			throw new JboratException(e);
		}
	}
	
	private String internClassName(String normalClassName) {
		
		final char NORMAL_DELIM = '.';
		final char INTERN_DELIM = '/';
		
		return normalClassName.replace(NORMAL_DELIM, INTERN_DELIM);
	}
	
	private void instrumentationLoop() throws JboratException, DiSLException {

		try {
		
		while (true) {

			ClassAsBytes cab = sc.readClassAsBytes();

			// communication closed by the client
			if (cab == null) {
				return;
			}

			byte[] instrClass;

			try {
				
				// TODO jb - weave time stats
				
				instrClass = instrument(new String(cab.getName()),
						cab.getCode());
			}
			catch (DiSLException e) {

				// instrumentation error
				// send the client a description of the server-side error

				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));

				// error protocol:
				// class name contains the description of the server-side error
				// class code is an array of size zero
				String errMsg = "Error in class " + cab.getName() + ": "
						+ sw.toString();

				sc.sendClassAsBytes(new ClassAsBytes(errMsg.getBytes(),
						new byte[0]));

				throw e;
			}

			sc.sendClassAsBytes(new ClassAsBytes(cab.getName(), instrClass));
		}
		
		}
		catch (IOException e) {
			throw new JboratException(e);
		}
	}
	
	private byte[] instrument(String className, byte[] origCode)
			throws JboratException, DiSLException {
		
		// dump uninstrumented
		if (uninstrPath != null) {
			dump(className, origCode, uninstrPath);
		}

		// filter
		if (filterClass(className)) {

			if (debug) {
				System.out.println("Excluding " + className);
			}
			
			return origCode;
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
			throws JboratException {
		
		final String CLASS_DELIM = "/"; 
		final String CLASS_EXT = ".class";
		
		try {
		
			// extract the class name and package name
			int i = className.lastIndexOf(CLASS_DELIM);
			String onlyClassName = className.substring(i + 1);
			String packageName = className.substring(0, i + 1);
			
			// construct path to the class
			String pathWithPkg = path + File.pathSeparator + packageName;

			// create directories
			new File(pathWithPkg).mkdirs();

			// dump the class code
			FileOutputStream fo = new FileOutputStream(pathWithPkg
					+ onlyClassName + CLASS_EXT);
			fo.write(codeAsBytes);
			fo.close();
		}
		catch (FileNotFoundException e) {
			throw new JboratException(e);
		}
		catch (IOException e) {
			throw new JboratException(e);
		}
	}
}
