package ch.usi.dag.disl.example.classgen;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ACC_VOLATILE;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Type.INT_TYPE;

import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceClassVisitor;

public class ShadowClassGenerator {

	private final String shadowClassName;
	
	private final Class<?> shadowedClass, templateClass;

	private final ClassVisitor cv;

	private final ClassWriter cw;

	private static final int COMPUTE_NOTHING = 0;

	private static final int ACC = ACC_PUBLIC | ACC_SYNTHETIC | ACC_VOLATILE;

	public ShadowClassGenerator(String shadowClassName, Class<?> shadowedClass, Class<?> templateClass) {
		this.shadowClassName = shadowClassName;
		this.shadowedClass = shadowedClass;
		this.templateClass = templateClass;
		
		this.cw = new ClassWriter(COMPUTE_NOTHING);
		this.cv = new TraceClassVisitor(cw, new Textifier(), new PrintWriter(System.err));
	}

	public byte[] generate() {
		generateClass();
		return cw.toByteArray();
	}

	private void generateClass() {
		cv.visit(Opcodes.V1_6, ACC_PUBLIC, shadowClassName, null, Type.getInternalName(templateClass), null);

		generateNullaryConstructor();

		for (SyntheticIntegerFieldsUpdater<?> updater : SyntheticIntegerFieldsUpdater.getUpdaters(templateClass))
			generateIntegerFields(shadowedClass, updater);

		cv.visitEnd();
	}
	

	private void generateNullaryConstructor() {
		final MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
		mv.visitCode();
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, Type.getInternalName(templateClass), "<init>", "()V");
		mv.visitInsn(RETURN);
		mv.visitMaxs(1, 1);
		mv.visitEnd();
	}

	private void generateIntegerFields(Class<?> clazz, SyntheticIntegerFieldsUpdater<?> updater) {
		if (clazz != null) {
			generateIntegerFields(clazz.getSuperclass(), updater);

			for (Field field : clazz.getDeclaredFields())
				if (!Modifier.isStatic(field.getModifiers()))
					cv.visitField(ACC, updater.getSyntheticFieldName(field), INT_TYPE.getDescriptor(), null, null);
		}
	}
}
