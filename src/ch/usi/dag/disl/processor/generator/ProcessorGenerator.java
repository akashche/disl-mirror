package ch.usi.dag.disl.processor.generator;

import java.util.List;
import java.util.Map;

import org.objectweb.asm.tree.AbstractInsnNode;

import ch.usi.dag.disl.dislclass.processor.Processor;
import ch.usi.dag.disl.dislclass.snippet.ProcessorInvocation;
import ch.usi.dag.disl.dislclass.snippet.Snippet;
import ch.usi.dag.disl.dislclass.snippet.marker.MarkedRegion;
import ch.usi.dag.disl.exception.DiSLFatalException;

public class ProcessorGenerator {

	public static PIResolver compute(
			Map<Snippet, List<MarkedRegion>> snippetMarkings) {
		
		PIResolver pir = new PIResolver();
		
		// for each snippet
		for(Snippet snippet : snippetMarkings.keySet()) {
			
			Map<AbstractInsnNode, ProcessorInvocation> invokedProcs =
				snippet.getCode().getInvokedProcessors();
			
			for(MarkedRegion markedRegion : snippetMarkings.get(snippet)) {
			
				// for each processor defined in snippet
				for(AbstractInsnNode instr : invokedProcs.keySet()) {
				
					ProcessorInvocation prcInv = invokedProcs.get(instr);
					
					// NOTE: the result is automatically inserted into PIResolver
					switch(prcInv.getProcApplyType()) {
					
					case INSIDE_METHOD: {
						computeInsideMethod(pir, snippet, markedRegion, instr,
								prcInv.getProcessor());
					}
					
					case BEFORE_INVOCATION: {
						computeBeforeInvocation(pir, snippet, markedRegion,
								instr, prcInv.getProcessor());
					}
					
					default:
						throw new DiSLFatalException(
								"Processor computation not defined");
					}
				}
			}
		}
		
		return pir;
	}

	private static void computeInsideMethod(PIResolver pir, Snippet snippet,
			MarkedRegion markedRegion, AbstractInsnNode instr,
			Processor processor) {
		// TODO ! processor - implement
		
	}
	
	private static void computeBeforeInvocation(PIResolver pir, Snippet snippet,
			MarkedRegion markedRegion, AbstractInsnNode instr,
			Processor processor) {
		// TODO ! processor - implement
		
	}
}
