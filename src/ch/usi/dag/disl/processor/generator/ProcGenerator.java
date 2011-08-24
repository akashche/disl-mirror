package ch.usi.dag.disl.processor.generator;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
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
import ch.usi.dag.disl.processor.ProcessorApplyType;

public class ProcGenerator {

	Map<Proc, ProcInstance> insideMethodPIs = new HashMap<Proc, ProcInstance>();

	public PIResolver compute(String fullMethodName, MethodNode methodNode,
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

					ProcInstance prcInst = null;

					// handle apply type
					switch (prcInv.getProcApplyType()) {

					case INSIDE_METHOD: {
						prcInst = computeInsideMethod(methodNode,
								prcInv.getProcessor());
						break;
					}

					case BEFORE_INVOCATION: {
						prcInst = computeBeforeInvocation(fullMethodName,
								markedRegion, prcInv.getProcessor());
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
			Proc processor) {

		// all instances of inside method processor will be the same
		// if we have one, we can use it multiple times

		ProcInstance procInst = insideMethodPIs.get(processor);

		if (procInst == null) {
			procInst = createProcInstance(ProcessorApplyType.INSIDE_METHOD,
					methodNode.desc, processor);
		}

		return procInst;
	}

	private ProcInstance computeBeforeInvocation(String fullMethodName,
			MarkedRegion markedRegion, Proc processor)
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
				methodInvocation.desc, processor);
	}

	private ProcInstance createProcInstance(ProcessorApplyType procApplyType,
			String methodDesc, Proc processor) {

		List<ProcMethodInstance> procMethodInstances = 
			new LinkedList<ProcMethodInstance>();

		// get argument types
		Type[] argTypeArray = Type.getArgumentTypes(methodDesc);
		
		// create processor method for each argument if applicable
		for (int i = 0; i < argTypeArray.length; ++i) {

			ProcMethodInstance pmi = createMethodInstance(i,
					argTypeArray.length, argTypeArray[i], processor);

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
			Type argType, Proc processor) {

		// TODO ! processors - arrays
		
		// traverse all methods and find the proper one
		for (ProcMethod method : processor.getMethods()) {
			
			ProcArgType methodArgType = ProcArgType.valueOf(argType);
			
			if(methodArgType == method.getType()) {
				return new ProcMethodInstance(argPos, argsCount, methodArgType,
						method.getCode());
			}
		}
		
		// no method with suitable type found
		return null;
	}
}
