package ch.usi.dag.disl.guard;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.processor.generator.ProcMethodInstance;
import ch.usi.dag.disl.snippet.MarkedRegion;
import ch.usi.dag.disl.snippet.ProcInvocation;
import ch.usi.dag.disl.snippet.Snippet;

public interface ProcessorMethodGuard {

	public boolean isApplicable(ClassNode classNode, MethodNode methodNode,
			Snippet snippet, MarkedRegion markedRegion,
			ProcInvocation processorInvocation,
			ProcMethodInstance processorMethodInstance,
			Type exactType);
}
