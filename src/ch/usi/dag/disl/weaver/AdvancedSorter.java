package ch.usi.dag.disl.weaver;

import java.util.Collections;
import java.util.Comparator;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.TryCatchBlockSorter;
import org.objectweb.asm.tree.TryCatchBlockNode;

import ch.usi.dag.disl.util.AsmHelper;

public class AdvancedSorter extends TryCatchBlockSorter {

	public AdvancedSorter(MethodVisitor mv, int access, String name,
			String desc, String signature, String[] exceptions) {
		super(mv, access, name, desc, signature, exceptions);
	}

	public void visitEnd() {
		// Compares TryCatchBlockNodes by the length of their "try" block.
		Comparator<TryCatchBlockNode> comp = new Comparator<TryCatchBlockNode>() {

			public int compare(TryCatchBlockNode t1, TryCatchBlockNode t2) {
				int len1 = blockLength(t1);
				int len2 = blockLength(t2);
				return len1 - len2;
			}

			private int blockLength(TryCatchBlockNode block) {
				int startidx = instructions.indexOf(AsmHelper.skipLabels(
						block.start, true));
				int endidx = instructions.indexOf(block.end);
				return endidx - startidx;
			}
		};
		
		Collections.sort(tryCatchBlocks, comp);
	}
}
