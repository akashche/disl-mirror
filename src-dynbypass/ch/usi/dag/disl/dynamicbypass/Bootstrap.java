package ch.usi.dag.disl.dynamicbypass;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;

public class Bootstrap {

	public static void completed(Instrumentation instr) {
		
		try {
			// get class loader
			ClassLoader cl = Bootstrap.class.getClassLoader();
			
			// find our class in resources
			InputStream dbcIS = 
					cl.getResourceAsStream("DynamicBypass-DynamicBypassCheck");
			
			byte[] newDBCCode = loadAsBytes(dbcIS);
			instr.redefineClasses(new ClassDefinition(DynamicBypassCheck.class, newDBCCode));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// thx: http://www.java2s.com/Tutorial/Java/0180__File/Loadfiletobytearray.htm
	public final static byte[] loadAsBytes(InputStream is) throws IOException {

		byte readBuf[] = new byte[512 * 1024];

		ByteArrayOutputStream bout = new ByteArrayOutputStream();

		int readCnt = is.read(readBuf);
		while (0 < readCnt) {
			bout.write(readBuf, 0, readCnt);
			readCnt = is.read(readBuf);
		}

		is.close();

		return bout.toByteArray();
	}
}
