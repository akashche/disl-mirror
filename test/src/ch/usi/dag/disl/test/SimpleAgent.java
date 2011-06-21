package ch.usi.dag.disl.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import ch.usi.dag.disl.DiSL;

public class SimpleAgent {
	public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException {
		ClassReader cr = new ClassReader(
				SimpleAgent.class.getResourceAsStream("TargetClass.class"));
		ClassNode cn = new ClassNode();
		cr.accept(cn, 0);
		
		DiSL.class.newInstance().instrument(cn);
		
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		cn.accept(cw);
		File f = new File("TargetClass.class");
		FileOutputStream fos = new FileOutputStream(f);
		fos.write(cw.toByteArray());
		fos.flush();
		fos.close();
	}
}
