package ch.usi.dag.dislreserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import ch.usi.dag.dislreserver.exception.DiSLREServerException;
import ch.usi.dag.dislreserver.reqdispatch.RequestDispatcher;

public abstract class DiSLREServer {

	public static final String PROP_DEBUG = "debug";
	private static final boolean debug = Boolean.getBoolean(PROP_DEBUG);

	private static final String PROP_PORT = "dislreserver.port";
	private static final int DEFAULT_PORT = 11218;
	private static final int port = Integer.getInteger(PROP_PORT, DEFAULT_PORT);
	
	public static void main(String args[]) {

		try {

			if (debug) {
				System.out.println("DiSL-RE server is starting on port "
						+ port);
			}

			ServerSocket listenSocket = new ServerSocket(port);

			Socket socket = listenSocket.accept();

			if (debug) {
				System.out.println("Accpeting new connection from "
						+ socket.getInetAddress().toString());
			}
			
			analysisLoop(socket);
			
			socket.close();
			
			if (debug) {
				System.out.println("DiSL-RE server is shutting down");
			}
			
		}
		catch (IOException e) {
			reportError(new DiSLREServerException(e));
		}
		catch (Throwable e) {
			reportError(e);
		}
	}

	private static void analysisLoop(Socket sock) throws DiSLREServerException {
		
		try {

			final DataInputStream is = new DataInputStream(
					new BufferedInputStream(sock.getInputStream()));
			final DataOutputStream os = new DataOutputStream(
					new BufferedOutputStream(sock.getOutputStream()));

			boolean exit = false;
			do {
				
				int requestNo = is.readInt();
				
				exit = RequestDispatcher.dispatch(requestNo, is, os, debug);
				
			} while(! exit);

		} catch (IOException e) {
			throw new DiSLREServerException(e);
		}
	}

	private static void reportInnerError(Throwable e) {
		
		Throwable cause = e.getCause();
		
		while (cause != null && cause.getMessage() != null) {
			System.err.println("  Inner error: " + cause.getMessage());
			cause = cause.getCause();
		}
	}
	
	private static void reportError(Throwable e) {

		if (e instanceof DiSLREServerException) {

			System.err.println("DiSL-RE server error: " + e.getMessage());

			reportInnerError(e);
			
			if (debug) {
				e.printStackTrace();
			}
			
			return;
		}

		// fatal exception (unexpected)
		System.err.println("DiSL-RE fatal error: " + e.getMessage());

		e.printStackTrace();
	}
}
