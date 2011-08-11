package ch.usi.dag.disl.util.stack;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceInterpreter;
import org.objectweb.asm.tree.analysis.SourceValue;

public class StackUtil {

	public static Analyzer<BasicValue> getBasicAnalyzer() {
		return new Analyzer<BasicValue>(new BasicVerifier());
	}

	public static int getOffset(Frame<BasicValue> frame) {
		int offset = 0;

		for (int i = frame.getStackSize() - 1; i >= 0; i--) {

			BasicValue v = frame.getStack(i);
			offset += v.getSize();
		}

		return offset;
	}

	public static InsnList enter(Frame<BasicValue> frame, int offset) {

		InsnList ilst = new InsnList();

		for (int i = frame.getStackSize() - 1; i >= 0; i--) {

			BasicValue v = frame.getStack(i);

			ilst.add(new VarInsnNode(v.getType().getOpcode(Opcodes.ISTORE),
					offset));
			offset += v.getSize();
		}

		return ilst;
	}

	public static InsnList exit(Frame<BasicValue> frame, int offset) {
		InsnList ilst = new InsnList();
		ilst.add(new LabelNode());

		for (int i = frame.getStackSize() - 1; i >= 0; i--) {

			BasicValue v = frame.getStack(i);

			ilst.insertBefore(ilst.getFirst(), new VarInsnNode(v.getType()
					.getOpcode(Opcodes.ILOAD), offset));
			offset += v.getSize();
		}

		return ilst;
	}

	public static Analyzer<SourceValue> getSourceAnalyzer() {
		return new Analyzer<SourceValue>(new SourceInterpreter());
	}
	
	public static SourceValue getSource(Frame<SourceValue> frame, int depth) {
		return frame.getStack(frame.getStackSize() - 1 - depth);
	}

}
