package ch.usi.dag.dislserver;

import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicInteger;

import ch.usi.dag.disl.DiSL;
import ch.usi.dag.disl.exception.DiSLException;

public abstract class InstrumentationServer {

	public static final String PROP_DEBUG = "debug";
	private static final boolean debug = Boolean.getBoolean(PROP_DEBUG);

	private static final String PROP_PORT = "dislserver.port";
	private static final int DEFAULT_PORT = 11217;
	private static final int port = Integer.getInteger(PROP_PORT, DEFAULT_PORT);

	private static final AtomicInteger aliveWorkers = new AtomicInteger();
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

				NetClassReader sc = 
						new NetClassReader(listenSocket.accept());
				
				aliveWorkers.incrementAndGet();
				
				new Worker(sc, disl).start();

				if (debug) {
					System.out.println("New connection accepted...");
				}
			}
			
		} catch (Exception e) {
			reportError(e);
		}
	}

	public static void reportError(Throwable e) {

		if (e instanceof DiSLException) {

			// Error reported in DiSL - just exit
			return;
		}

		if (e instanceof DiSLServerException) {

			System.err.println("DiSL server error: " + e.getMessage());

			if (debug) {
				e.printStackTrace();
			}
		}

		// fatal exception (unexpected)
		System.err.println("Fatal error: " + e.getMessage());

		e.printStackTrace();
	}
	
	public static void workerDone() {

		if (aliveWorkers.decrementAndGet() == 0) {
			
			disl.terminate();
			
			if (debug) {
				System.out.println("Instrumentation server is shutting down");
			}
			
			// no workers - shutdown
			System.exit(0);
		}
	}
}
