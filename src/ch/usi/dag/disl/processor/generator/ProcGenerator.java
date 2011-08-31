package ch.usi.dag.disl.processor.generator;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.dislclass.processor.Proc;
import ch.usi.dag.disl.dislclass.processor.ProcArgType;
import ch.usi.dag.disl.dislclass.processor.ProcMethod;
import ch.usi.dag.disl.dislclass.snippet.ProcInvocation;
import ch.usi.dag.disl.dislclass.snippet.Snippet;
import ch.usi.dag.disl.dislclass.snippet.marker.MarkedRegion;
import ch.usi.dag.disl.exception.DiSLFatalException;
import ch.usi.dag.disl.exception.ProcessorException;
import ch.usi.dag.disl.guard.ProcessorGuard;
import ch.usi.dag.disl.guard.ProcessorMethodGuard;
import ch.usi.dag.disl.processor.ProcessorApplyType;

public class ProcGenerator {

	Map<Proc, ProcInstance> insideMethodPIs = new HashMap<Proc, ProcInstance>();

	private class PMGuardData {
		
		private ClassNode classNode;
		private MethodNode methodNode;
		private Snippet snippet;
		private MarkedRegion markedRegion;
		private ProcInvocation prcInv;
		
		public PMGuardData(ClassNode classNode, MethodNode methodNode,
				Snippet snippet, MarkedRegion markedRegion,
				ProcInvocation prcInv) {
			super();
			this.classNode = classNode;
			this.methodNode = methodNode;
			this.snippet = snippet;
			this.markedRegion = markedRegion;
			this.prcInv = prcInv;
		}

		public ClassNode getClassNode() {
			return classNode;
		}

		public MethodNode getMethodNode() {
			return methodNode;
		}

		public Snippet getSnippet() {
			return snippet;
		}

		public MarkedRegion getMarkedRegion() {
			return markedRegion;
		}

		public ProcInvocation getPrcInv() {
			return prcInv;
		}
	}
	
	public PIResolver compute(ClassNode classNode, MethodNode methodNode,
			Map<Snippet, List<MarkedRegion>> snippetMarkings)
			throws ProcessorException {

		PIResolver piResolver = new PIResolver();

		// for each snippet
		for (Snippet snippet : snippetMarkings.keySet()) {

			Map<Integer, ProcInvocation> invokedProcs = snippet.getCode()
					.getInvokedProcessors();

			for (MarkedRegion markedRegion : snippetMarkings.get(snippet)) {

				// for each processor defined in snippet
				for (Integer instrPos : invokedProcs.keySet()) {

					ProcInvocation prcInv = invokedProcs.get(instrPos);

					Proc processor = prcInv.getProcessor();
					ProcessorGuard prcGuard = processor.getGuard();

					// evaluate processor guard
					if(prcGuard != null && ! prcGuard.isApplicable(classNode,
							methodNode, snippet, markedRegion, prcInv)) {
						// if not applicable - skipp
						continue;
					}
					
					ProcInstance prcInst = null;
					PMGuardData pmgd = new PMGuardData(classNode, methodNode,
							snippet, markedRegion, prcInv);

					// handle apply type
					switch (prcInv.getProcApplyType()) {

					case INSIDE_METHOD: {
						prcInst = computeInsideMethod(methodNode,
								prcInv.getProcessor(), pmgd);
						break;
					}

					case BEFORE_INVOCATION: {
						prcInst = computeBeforeInvocation(
								classNode.name + "." + methodNode.name,
								markedRegion, prcInv.getProcessor(), pmgd);
						break;
					}

					default:
						throw new DiSLFatalException(
								"Proc computation not defined");
					}
					
					if(prcInst != null) {
						// add result to processor instance resolver
						piResolver.set(snippet, markedRegion, instrPos, prcInst);
					}
				}
			}
		}

		return piResolver;
	}

	private ProcInstance computeInsideMethod(MethodNode methodNode,
			Proc processor, PMGuardData pmgd) {

		// all instances of inside method processor will be the same
		// if we have one, we can use it multiple times

		ProcInstance procInst = insideMethodPIs.get(processor);

		if (procInst == null) {
			procInst = createProcInstance(ProcessorApplyType.INSIDE_METHOD,
					methodNode.desc, processor, pmgd);
		}

		return procInst;
	}

	private ProcInstance computeBeforeInvocation(String fullMethodName,
			MarkedRegion markedRegion, Proc processor, PMGuardData pmgd)
			throws ProcessorException {

		// NOTE: SnippetUnprocessedCode checks that BEFORE_INVOCATION is
		// used only with BytecodeMarker
		
		// because it is BytecodeMarker, it should have only one end 
		if(markedRegion.getEnds().size() > 1) {
			throw new DiSLFatalException(
					"Expected only one end in marked region");
		}
		
		// get instruction from the method code
		// the method invocation is the instruction marked as end
		AbstractInsnNode instr = markedRegion.getEnds().get(0);

		// check - method invocation
		if (!(instr instanceof MethodInsnNode)) {
			throw new ProcessorException("Processor " + processor.getName()
					+ " is not applied before method invocation in method "
					+ fullMethodName);
		}

		MethodInsnNode methodInvocation = (MethodInsnNode) instr;

		return createProcInstance(ProcessorApplyType.BEFORE_INVOCATION,
				methodInvocation.desc, processor, pmgd);
	}

	private ProcInstance createProcInstance(ProcessorApplyType procApplyType,
			String methodDesc, Proc processor, PMGuardData pmgd) {

		List<ProcMethodInstance> procMethodInstances = 
			new LinkedList<ProcMethodInstance>();

		// get argument types
		Type[] argTypeArray = Type.getArgumentTypes(methodDesc);
		
		// create processor method for each argument if applicable
		for (int i = 0; i < argTypeArray.length; ++i) {

			ProcMethodInstance pmi = createMethodInstance(i,
					argTypeArray.length, argTypeArray[i], processor, pmgd);

			// if processor method was created, add it
			if (pmi != null) {
				procMethodInstances.add(pmi);
			}

		}

		if(procMethodInstances.isEmpty()) {
			return null;
		}

		// create new processor instance
		return new ProcInstance(procApplyType, procMethodInstances);
	}

	private ProcMethodInstance createMethodInstance(int argPos, int argsCount,
			Type argType, Proc processor, PMGuardData pmgd) {

		ProcArgType methodArgType = ProcArgType.valueOf(argType);
		
		// traverse all methods and find the proper one
		for (ProcMethod method : processor.getMethods()) {
			
			if(methodArgType == method.getType()) {
				
				ProcMethodInstance pmi = new ProcMethodInstance(argPos,
						argsCount, methodArgType, method.getCode());
				
				return evaluateProcessorMethodGuard(method.getGuard(), pmgd,
						pmi, argType);
			}
		}
		
		// no method with suitable type found
		return null;
	}

	private ProcMethodInstance evaluateProcessorMethodGuard(
			ProcessorMethodGuard guard, PMGuardData pmgd,
			ProcMethodInstance pmi, Type exactType) {

		// evaluate processor method guard
		if(guard != null && ! guard.isApplicable(pmgd.getClassNode(),
				pmgd.getMethodNode(), pmgd.getSnippet(), pmgd.getMarkedRegion(),
				pmgd.getPrcInv(), pmi, exactType)) {
			
			// if not applicable
			return null;
		}
		
		return pmi;
	}
}
