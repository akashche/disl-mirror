package ch.usi.dag.disl.test2.utils;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class ClientServerRunner {

	private Class<?> c;
	private Job client;
	private Job server;
	
	public ClientServerRunner(Class<?> c) {
		this.c = c;
	}
	
	public Job getClient() {
		return client;		
	}
	
	public Job getServer() {
		return server;		
	}
	
	private String getTestName() {
		String[] packages = this.c.getPackage().getName().split("\\.");
		return packages[packages.length-2];
	}
	
	public void start() 
			throws IOException {
		String test = getTestName();
		
		String[] serverCmd = new String[] {
				"java", 
				"-cp", "build-test/disl-instr-"+test+".jar:build/disl-server.jar",
				"ch.usi.dag.dislserver.DiSLServer"  
		};
		server = new Job(serverCmd);
		server.execute();	
		
		server.waitFor(3000);
		
		String[] clientCmd = new String[] {
				"java", 
				"-agentpath:build/libdislagent.so",
				"-javaagent:build/disl-agent.jar", 
				"-Xbootclasspath/a:build/disl-agent.jar:build-test/disl-instr-"+test+".jar",
				"-jar", "build-test/disl-app-"+test+".jar"
		};
		client = new Job(clientCmd);
		client.execute();		
	}
	
	public boolean waitFor(long milliseconds) {
		boolean finished = true;		
		finished = finished & client.waitFor(milliseconds);
		finished = finished & server.waitFor(milliseconds);
		return finished;
	}
	
	public boolean waitFor() {
		return waitFor(60000);
	}
		
	public void assertIsNotRunning() {
		assertFalse("client still running", client.isRunning());
		assertFalse("server still running", server.isRunning());
	}	
			
	public void assertIsFinished() {
		assertTrue("client not finished", client.isFinished());
		assertTrue("server not finished", server.isFinished());
	}	

	public void writeFile(String filename, String str) 
			throws FileNotFoundException {
		try (PrintWriter out = new PrintWriter(filename)) {
			out.print(str);
		}
	}
	
	public void flushClientOut(String filename) 
			throws FileNotFoundException, IOException {
		writeFile(filename, client.getOutput());
	}
	
	public void flushClientOut() 
			throws FileNotFoundException, IOException {
		flushClientOut("tmp."+getTestName()+".out.txt");
	}
	
	public void flushClientErr(String filename) 
			throws FileNotFoundException, IOException {
		writeFile(filename, client.getError());
	}
	
	public void flushClientErr() 
			throws FileNotFoundException, IOException {
		flushClientErr("tmp."+getTestName()+".err.txt");
	}
	
	public void flushServerOut(String filename) 
			throws FileNotFoundException, IOException {
		writeFile(filename, server.getOutput());
	}

	public void flushServerErr(String filename) 
			throws FileNotFoundException, IOException {
		writeFile(filename, server.getError());
	}	
	
	public String getResource(String filename) 
			throws IOException {
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(this.c.getResourceAsStream(filename), "UTF-8"));) {
			
			StringBuffer buffer = new StringBuffer();
			for (int c = reader.read(); c != -1; c = reader.read()) 
				buffer.append((char)c);

			return buffer.toString();
		}
	}
	
	public void assertClientOut(String filename) 
			throws IOException {
		assertEquals("client out does not match", getResource(filename), client.getOutput());
	}
	
	public void assertClientOutNull() 
			throws IOException {
		assertEquals("client out does not match", "", client.getOutput());
	}

	public void assertClientErr(String filename) 
			throws IOException {
		assertEquals("client err does not match", getResource(filename), client.getError());
	}
	
	public void assertClientErrNull() 
			throws IOException {
		assertEquals("client err does not match", "", client.getError());
	}

	public void assertServerOut(String filename)
			throws IOException {
		assertEquals("server out does not match", getResource(filename), server.getOutput());
	}
	
	public void assertServerOutNull()
			throws IOException {
		assertEquals("server out does not match", "", server.getOutput());
	}

	public void assertServerErr(String filename)
			throws IOException {
		assertEquals("server err does not match", getResource(filename), server.getError());
	}
	
	public void assertServerErrNull()
			throws IOException {
		assertEquals("server err does not match", "", server.getError());
	}
	
	public void verbose() 
			throws IOException {
		System.out.println("client-out:");
		System.out.println(client.getOutput());
		System.out.println("client-err:");
		System.out.println(client.getError());
		System.out.println("server-out:");
		System.out.println(server.getOutput());
		System.out.println("server-err:");
		System.out.println(server.getError());
	}
	
	public void destroy() {
		client.destroy();
		server.destroy();
	}
}
