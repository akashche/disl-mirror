package ch.usi.dag.disl.test.guard;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.snippet.MarkedRegion;
import ch.usi.dag.disl.snippet.ProcInvocation;
import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.guard.ProcessorGuard;
import ch.usi.dag.disl.guard.ProcessorMethodGuard;
import ch.usi.dag.disl.guard.SnippetGuard;
import ch.usi.dag.disl.processor.generator.ProcMethodInstance;

public class GuardNo implements SnippetGuard, ProcessorGuard, ProcessorMethodGuard {

	@Override
	public boolean isApplicable(ClassNode classNode, MethodNode methodNode,
			Snippet snippet, MarkedRegion markedRegion,
			ProcInvocation processorInvocation,
			ProcMethodInstance processorMethodInstance, Type exactType) {

		return false;
	}

	@Override
	public boolean isApplicable(ClassNode classNode, MethodNode methodNode,
			Snippet snippet, MarkedRegion markedRegion, ProcInvocation prcInv) {

		return false;
	}

	@Override
	public boolean isApplicable(ClassNode classNode, MethodNode methodNode,
			Snippet snippet, MarkedRegion markedRegion) {

		return false;
	}

}
