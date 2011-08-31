package ch.usi.dag.disl.test.guard;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.dislclass.snippet.ProcInvocation;
import ch.usi.dag.disl.dislclass.snippet.Snippet;
import ch.usi.dag.disl.dislclass.snippet.marker.MarkedRegion;
import ch.usi.dag.disl.guard.ProcessorGuard;
import ch.usi.dag.disl.guard.ProcessorMethodGuard;
import ch.usi.dag.disl.guard.SnippetGuard;
import ch.usi.dag.disl.processor.generator.ProcMethodInstance;

public class GuardYes implements SnippetGuard, ProcessorGuard, ProcessorMethodGuard {

	@Override
	public boolean isApplicable(ClassNode classNode, MethodNode methodNode,
			Snippet snippet, MarkedRegion markedRegion,
			ProcInvocation processorInvocation,
			ProcMethodInstance processorMethodInstance, Type exactType) {

		return true;
	}

	@Override
	public boolean isApplicable(ClassNode classNode, MethodNode methodNode,
			Snippet snippet, MarkedRegion markedRegion, ProcInvocation prcInv) {

		return true;
	}

	@Override
	public boolean isApplicable(ClassNode classNode, MethodNode methodNode,
			Snippet snippet, MarkedRegion markedRegion) {

		return true;
	}

}
