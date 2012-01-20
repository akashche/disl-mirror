package ch.usi.dag.disl.testtools.onplace;

import java.io.FileOutputStream;
import java.io.PrintWriter;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import ch.usi.dag.disl.DiSL;

public class OnPlaceTransformer {
	
	public static void main(String[] args) throws Exception {
		
		// INSTRUCTIONS: Under Eclipse call me with these jvm params (example)
		// -Ddisltest.transform=bin/ch/usi/dag/disl/test/bodymarker/TargetClass.class
		// -Ddisl.classes=bin/ch/usi/dag/disl/test/bodymarker/DiSLClass.class
		
		// do not use dynamic bypass
		DiSL disl = new DiSL(false);
		
		String classToTransform = null;
		
		if(args.length == 1) {
			classToTransform = args[0];
		}
		
		if(classToTransform == null) {
			classToTransform = System.getProperty("disltest.transform");
		}
		
		if(classToTransform == null) {
			System.err.println("No class to transform...");
			System.exit(1);
		}

		// get code as bytes
		byte[] origCode = null;
		
		/* TODO java 7 only
		Path cttPath = FileSystems.getDefault().getPath(classToTransform);
		origCode = Files.readAllBytes(cttPath);
		/**/
		
		// check class first
		ClassReader cr = new ClassReader(origCode);
		cr.accept(new CheckClassAdapter(
				new TraceClassVisitor(new PrintWriter(System.out))), 0);
		
		// instrument class
		byte[] instrCode = disl.instrument(origCode);
		
		if(instrCode != null) {
		
			FileOutputStream fos = new FileOutputStream("ModifiedClass.class");
			fos.write(instrCode);
			fos.close();
		}
	}
}
