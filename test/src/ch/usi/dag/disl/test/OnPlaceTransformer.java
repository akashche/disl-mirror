package ch.usi.dag.disl.test;

import java.io.File;
import java.io.FileOutputStream;

import ch.usi.dag.disl.DiSL;

public class OnPlaceTransformer {
	
	public static void main(String[] args) throws Exception {
		
		// INSTRUCTIONS: Under Eclipse call me with these jvm params (example)
		// -Dtest.class=/ch/usi/dag/disl/test/bodymarker/TargetClass.class
		// -Ddisl.classes=bin/ch/usi/dag/disl/test/bodymarker/DiSLClass.class
		
		DiSL disl = new DiSL();
		disl.initialize();
		
		byte[] instrOut = disl.instrument(OnPlaceTransformer.class
				.getResourceAsStream(System.getProperty("test.class")));
		
		if(instrOut != null) {
		
			File f = new File("ModifiedClass.class");
			FileOutputStream fos = new FileOutputStream(f);
			fos.write(instrOut);
			fos.flush();
			fos.close();
		}
	}
}
