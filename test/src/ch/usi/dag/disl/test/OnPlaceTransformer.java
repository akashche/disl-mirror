package ch.usi.dag.disl.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.util.TraceClassVisitor;

import ch.usi.dag.disl.DiSL;

public class OnPlaceTransformer {
	
	public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException {
		
		// INSTRUCTIONS: Under Eclipse call me with these jvm params (example)
		// -Dtest.class=/ch/usi/dag/disl/test/bodymarker/TargetClass.class
		// -Ddisl.classes=bin/ch/usi/dag/disl/test/bodymarker/DiSLClass.class
		
		ClassReader cr = new ClassReader(
				OnPlaceTransformer.class.getResourceAsStream(
						System.getProperty("test.class")));
		ClassNode cn = new ClassNode();
		cr.accept(cn, 0);
		
		DiSL disl = new DiSL();
		disl.initialize();
		disl.instrument(cn);
		
		// print class
		TraceClassVisitor tcv = new TraceClassVisitor(new PrintWriter(System.out));
		cn.accept(tcv);
		
		// output it into file
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		cn.accept(cw);
		
		File f = new File("ModifiedClass.class");
		FileOutputStream fos = new FileOutputStream(f);
		fos.write(cw.toByteArray());
		fos.flush();
		fos.close();
	}
}
