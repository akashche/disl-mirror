package ch.usi.dag.dislserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import ch.usi.dag.disl.DiSL;
import ch.usi.dag.disl.exception.DiSLException;

public abstract class DiSLServer {

	public static final String PROP_DEBUG = "debug";
	private static final boolean debug = Boolean.getBoolean(PROP_DEBUG);

	private static final String PROP_PORT = "dislserver.port";
	private static final int DEFAULT_PORT = 11217;
	private static final int port = Integer.getInteger(PROP_PORT, DEFAULT_PORT);
	
	private static final String PROP_TIME_STAT = "dislserver.timestat";
	private static final boolean timeStat = Boolean.getBoolean(PROP_TIME_STAT);
	
	private static final String PROP_CONT = "dislserver.continuous";
	private static final boolean continuous = Boolean.getBoolean(PROP_CONT);

	private static final AtomicInteger aliveWorkers = new AtomicInteger();
	private static final AtomicLong instrumentationTime = new AtomicLong();
	
	private static DiSL disl;

	public static void main(String args[]) {

		try {

			// use dynamic bypass
			disl = new DiSL(true);

			if (debug) {
				System.out.println("Instrumentation server is starting on port "
						+ port);
			}

			ServerSocket listenSocket = new ServerSocket(port);

			while (true) {

				Socket newClient = listenSocket.accept();
				
				NetMessageReader sc = new NetMessageReader(newClient);
				
				aliveWorkers.incrementAndGet();
				
				new Worker(sc, disl).start();

				if (debug) {
					System.out.println("Accpeting new connection from "
							+ newClient.getInetAddress().toString());
				}
			}
			
		}
		catch (IOException e) {
			reportError(new DiSLServerException(e));
		}
		catch (Throwable e) {
			reportError(e);
		}
	}

	private static void reportInnerError(Throwable e) {
	
		Throwable cause = e.getCause();
		
		while (cause != null && cause.getMessage() != null) {
			System.err.println("  Inner error: " + cause.getMessage());
			cause = cause.getCause();
		}
	}
	
	public static void reportError(Throwable e) {

		if (e instanceof DiSLException) {

			System.err.println("DiSL error: " + e.getMessage());
			
			reportInnerError(e);
			
			if(debug) {
				e.printStackTrace();
			}

			return;
		}

		if (e instanceof DiSLServerException) {

			System.err.println("DiSL server error: " + e.getMessage());

			reportInnerError(e);
			
			if (debug) {
				e.printStackTrace();
			}
			
			return;
		}

		// fatal exception (unexpected)
		System.err.println("DiSL fatal error: " + e.getMessage());

		e.printStackTrace();
	}
	
	public static void workerDone(long instrTime) {

		instrumentationTime.addAndGet(instrTime);
		
		if (aliveWorkers.decrementAndGet() == 0) {
			
			if (timeStat) {
				System.out.println("Instrumentation took " +
						instrumentationTime.get() / 1000000 + " milliseconds");
			}
			
			// no workers - shutdown
			if(! continuous) {

				disl.terminate();
				
				if (debug) {
					System.out.println("Instrumentation server is shutting down");
				}
				
				System.exit(0);				
			}
		}
	}
}
