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
    	
    	byte[] instrumentedClass = null;
    	
    	try {
    	
	    	ClassReader cr = new ClassReader(classfileBuffer);
			ClassNode classNode = new ClassNode();
			cr.accept(classNode, ClassReader.SKIP_DEBUG | ClassReader.EXPAND_FRAMES);
	    	
			DiSL disl = new DiSL();
			disl.initialize();
			disl.instrument(classNode);
				
			ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
			classNode.accept(cw);
	
			instrumentedClass = cw.toByteArray();
			
			// TODO enable after jborat interface change
			/*
			// print class 
			TraceClassVisitor tcv = new TraceClassVisitor(new PrintWriter(System.out));
			classNode.accept(tcv);
			/**/
			
			/*
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
			/**/
			
    	} catch(Throwable e) {
    		e.printStackTrace();
    	}
    	
        return instrumentedClass;
    }

}
