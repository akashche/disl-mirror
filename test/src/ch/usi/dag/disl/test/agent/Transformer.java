package ch.usi.dag.disl.test.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import ch.usi.dag.disl.DiSL;

public class Transformer implements ClassFileTransformer {
    
    @Override
    public byte [] transform (ClassLoader loader, String className,
        Class <?> classBeingRedefined, ProtectionDomain protectionDomain,
        byte [] classfileBuffer)
        throws IllegalClassFormatException {
        
    	ClassReader cr = new ClassReader(classfileBuffer);
		ClassNode classNode = new ClassNode();
		cr.accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.EXPAND_FRAMES);
    	
		DiSL disl = new DiSL();
		disl.initialize();
		disl.instrument(classNode);

		
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		classNode.accept(cw);

		byte[] instrumentedClass = cw.toByteArray();
		
		// TODO Remove the two-round computing
		cr = new ClassReader(instrumentedClass);
		cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		cr.accept(cw, ClassReader.SKIP_DEBUG | ClassReader.EXPAND_FRAMES);
		instrumentedClass = cw.toByteArray();
		
		/* now does for every user class - unusable
		 * unable to track wheter is modified or not
		// print class 
		TraceClassVisitor tcv = new TraceClassVisitor(new PrintWriter(System.out));
		classNode.accept(tcv);

		// output class
		try {
			File f = new File("ModifiedClass.class");
			FileOutputStream fos = new FileOutputStream(f);
			fos.write(cw.toByteArray());
			fos.flush();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		*/
		
        return instrumentedClass;
    }

}
