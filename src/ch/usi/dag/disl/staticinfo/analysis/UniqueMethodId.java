package ch.usi.dag.disl.staticinfo.analysis;

import java.io.FileNotFoundException;
import java.io.PrintWriter;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.exception.StaticAnalysisException;
import ch.usi.dag.disl.staticinfo.analysis.cache.MethodCache;
import ch.usi.dag.disl.util.Constants;

public class UniqueMethodId extends AbstractStaticAnalysis {

	public UniqueMethodId() {

		// don't delete - id calculation depends on it
		registerCache("get", MethodCache.class);
	}

	// singleton class that holds IDs
	private static class IdHolder {
		
		private static final String DEFAULT_OUTPUT_FILE = "methodid.txt";

		private static IdHolder instance = null;
		
		private PrintWriter output = null;
		
		static {
			
		}

		private int nextId = 0;
		
		private IdHolder() {

			String outputFileName = 
				System.getProperty("disl.analysis.umidfile");
			
			if(outputFileName == null) {
				outputFileName = DEFAULT_OUTPUT_FILE;
			}
			
			// create output
			try {
				output = new PrintWriter(outputFileName);
			} catch (FileNotFoundException e) {
				throw new StaticAnalysisException(
						"Cannot create output for UniqueMethodId analysis", e);
			}
			
			// register shutdown hook - output close
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					output.close();
				}
			});
		}

		public static IdHolder getInstance() {
			if (instance == null) {
				instance = new IdHolder();
			}
			return instance;
		}
		
		public int getNewMethodID(String methodName) {
			
			int newId = nextId;
			++nextId;

			// dump to the file
			output.println(newId + "\t" + methodName);
			
			return newId;
		}
	}
	
	// override, if you want to define your own method name format
	protected String getMethodName(ClassNode classNode, MethodNode methodNode) {
		
		return classNode.name
				+ Constants.CLASS_DELIM
				+ methodNode.name
				+ "("
				+ methodNode.desc
				+ ")";
	}
	
	// if you want to override this method, register method cache for new method
	public int get() {

		// this method relays on method cache otherwise it can return different
		// id for the same method
		
		String methodName = getMethodName(staticAnalysisData.getClassNode(),
				staticAnalysisData.getMethodNode());
		return IdHolder.getInstance().getNewMethodID(methodName);
	}
}
