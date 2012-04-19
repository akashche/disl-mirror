package ch.usi.dag.disl.example.jcarder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.VarInsnNode;

import ch.usi.dag.disl.transformer.Transformer;
import ch.usi.dag.disl.util.Constants;
import ch.usi.dag.dislserver.DiSLServerException;

public class SynchronizedMethodsTransformer implements Transformer {

	final private static String instrPath = "transformed";
	final private static String CLASS_EXT = ".class";
	final private static char PACKAGE_STD_DELIM = '.';
	final private static boolean DEBUG = false; 
		
	@Override
	public byte[] transform(byte[] classfileBuffer) throws Exception {
		
		byte[] instrumentedBytes = null;
		ClassReader cr = new ClassReader(classfileBuffer);
		ClassNode clazz = new ClassNode(Opcodes.ASM4);
		cr.accept(clazz, 0);

		if(clazz.name.startsWith("java") 
				|| clazz.name.startsWith("sun")
				//|| clazz.name.contains("disl") 
				|| clazz.name.contains("com") 
				) {
			return classfileBuffer;
		}
		
		String className = clazz.name.replace('/', '.');
		for (MethodNode method : (List<MethodNode>) clazz.methods) {
			boolean isStatic = false;
			if ((method.access & Opcodes.ACC_ABSTRACT) != 0 
					|| (method.access & Opcodes.ACC_NATIVE) != 0) {
				continue;
			}
		
			if ((method.access & Opcodes.ACC_SYNCHRONIZED) != 0) {
				if(DEBUG)
					System.err.println("++++ Transfroming the method " + clazz.name +"." + method.name + " ++++");
				if ((method.access & Opcodes.ACC_STATIC) != 0) {
				 	isStatic = true;
				}
				method.access -= Opcodes.ACC_SYNCHRONIZED;
				InsnList instructions = method.instructions;

				final LabelNode startTryCatchBlockLabel = new LabelNode();
				final LabelNode endTryCatchBlockLabel = new LabelNode();

				InsnList myList = new InsnList();
				myList.add(startTryCatchBlockLabel);
				if(isStatic) {
					myList.add(new LdcInsnNode(className));
					myList.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
							"java/lang/Class",
							"forName",
							"(Ljava/lang/String;)Ljava/lang/Class;"));
				}
				else {
					myList.add(new VarInsnNode(Opcodes.ALOAD, 0));
				}
				myList.add(new InsnNode(Opcodes.MONITORENTER));

				instructions.insert(myList);
				Iterator<AbstractInsnNode> it = instructions.iterator();
				while(it.hasNext()) {
					AbstractInsnNode instruction = (AbstractInsnNode) it.next();
					int opcode = instruction.getOpcode();
					if(opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN)
					{
						myList.clear();
						if(isStatic) {
							myList.add(new LdcInsnNode(className));
							myList.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
									"java/lang/Class",
									"forName",
									"(Ljava/lang/String;)Ljava/lang/Class;"));
						}
						else {
							myList.add(new VarInsnNode(Opcodes.ALOAD, 0));
						}
						myList.add(new InsnNode(Opcodes.MONITOREXIT));
						
						instructions.insert(instruction.getPrevious(), myList);
					}
				}
				myList.clear();
				LabelNode normalExecutionFinallyBlockLabel = new LabelNode();
				myList.add(endTryCatchBlockLabel);
				method.tryCatchBlocks.add(new
						TryCatchBlockNode(startTryCatchBlockLabel, endTryCatchBlockLabel,
								endTryCatchBlockLabel, null));
				if(isStatic) {
					myList.add(new LdcInsnNode(className));
					myList.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
							"java/lang/Class",
							"forName",
							"(Ljava/lang/String;)Ljava/lang/Class;"));
				}
				else {
					myList.add(new VarInsnNode(Opcodes.ALOAD, 0));
				}
				myList.add(new InsnNode(Opcodes.MONITOREXIT));
				myList.add(new InsnNode(Opcodes.ATHROW));
				myList.add(normalExecutionFinallyBlockLabel);
				instructions.add(myList);
			}
		}
		
		ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		clazz.accept(cw);
		instrumentedBytes = cw.toByteArray();
		dump(clazz.name, instrumentedBytes, instrPath);
		
		return instrumentedBytes;
	}

	@Override
	public boolean propagateUninstrumentedClasses() {
		return false;
	}
	
	private void dump(String className, byte[] codeAsBytes, String path)
			throws DiSLServerException {
		
		try {
		
			// extract the class name and package name
			int i = className.lastIndexOf(Constants.PACKAGE_INTERN_DELIM);
			String onlyClassName = className.substring(i + 1);
			String packageName = className.substring(0, i + 1);
			
			// construct path to the class
			String pathWithPkg = path + File.separator + packageName;

			// create directories
			new File(pathWithPkg).mkdirs();

			// dump the class code
			FileOutputStream fo = new FileOutputStream(pathWithPkg
					+ onlyClassName + Constants.CLASS_EXT);
			fo.write(codeAsBytes);
			fo.close();
		}
		catch (FileNotFoundException e) {
			throw new DiSLServerException(e);
		}
		catch (IOException e) {
			throw new DiSLServerException(e);
		}
	}
}
