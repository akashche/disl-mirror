package ch.usi.dag.disl.weaver;

import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.AfterReturning;
import ch.usi.dag.disl.annotation.AfterThrowing;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.annotation.SyntheticLocal.Initialize;
import ch.usi.dag.disl.exception.DynamicContextException;
import ch.usi.dag.disl.localvar.SyntheticLocalVar;
import ch.usi.dag.disl.processor.generator.PIResolver;
import ch.usi.dag.disl.snippet.Shadow;
import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.snippet.SnippetCode;
import ch.usi.dag.disl.snippet.Shadow.WeavingRegion;
import ch.usi.dag.disl.staticcontext.generator.SCGenerator;
import ch.usi.dag.disl.util.AsmHelper;

// The weaver instruments byte-codes into java class. 
public class Weaver {

	// Transform static fields to synthetic local
	// NOTE that the field maxLocals of the method node will be automatically
	// updated.
	private static void static2Local(MethodNode methodNode,
			List<SyntheticLocalVar> syntheticLocalVars) {

		InsnList instructions = methodNode.instructions;

		// Extract the 'id' field in each synthetic local
		AbstractInsnNode first = instructions.getFirst();

		// Initialization
		// TODO respect BEST_EFFORT initialization type in synthetic local variable
		for (SyntheticLocalVar var : syntheticLocalVars) {

			if (var.getInitialize() == Initialize.NEVER) {
				continue;
			}

			if (var.getInitASMCode() != null) {

				InsnList newlst = AsmHelper.cloneInsnList(var.getInitASMCode());
				instructions.insertBefore(first, newlst);
			} else {

				Type type = var.getType();

				switch (type.getSort()) {
				case Type.ARRAY:
					instructions.insertBefore(first, new InsnNode(
							Opcodes.ACONST_NULL));
					instructions.insertBefore(first, new TypeInsnNode(
							Opcodes.CHECKCAST, type.getDescriptor()));
					break;

				case Type.BOOLEAN:
				case Type.BYTE:
				case Type.CHAR:
				case Type.INT:
				case Type.SHORT:
					instructions.insertBefore(first, new InsnNode(
							Opcodes.ICONST_0));
					break;

				case Type.DOUBLE:
					instructions.insertBefore(first, new InsnNode(
							Opcodes.DCONST_0));
					break;

				case Type.FLOAT:
					instructions.insertBefore(first, new InsnNode(
							Opcodes.FCONST_0));
					break;

				case Type.LONG:
					instructions.insertBefore(first, new InsnNode(
							Opcodes.LCONST_0));
					break;

				case Type.OBJECT:
				default:
					instructions.insertBefore(first, new InsnNode(
							Opcodes.ACONST_NULL));
					break;
				}

				instructions.insertBefore(first,
						new FieldInsnNode(Opcodes.PUTSTATIC, var.getOwner(),
								var.getName(), type.getDescriptor()));
			}
		}

		// Scan for FIELD instructions and replace with local load/store.
		for (AbstractInsnNode instr : instructions.toArray()) {
			int opcode = instr.getOpcode();

			// Only field instructions will be transformed.
			if (opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC) {

				FieldInsnNode field_instr = (FieldInsnNode) instr;
				String id = field_instr.owner + SyntheticLocalVar.NAME_DELIM
						+ field_instr.name;

				int index = 0, count = 0;

				for (SyntheticLocalVar var : syntheticLocalVars) {

					if (id.equals(var.getID())) {
						break;
					}

					index += var.getType().getSize();
					count++;
				}

				if (count == syntheticLocalVars.size()) {
					// Not contained in the synthetic locals.
					continue;
				}

				// Construct a instruction for local
				Type type = Type.getType(field_instr.desc);
				int new_opcode = type
						.getOpcode(opcode == Opcodes.GETSTATIC ? Opcodes.ILOAD
								: Opcodes.ISTORE);
				VarInsnNode insn = new VarInsnNode(new_opcode,
						methodNode.maxLocals + index);
				instructions.insertBefore(instr, insn);
				instructions.remove(instr);
			}
		}

		methodNode.maxLocals += syntheticLocalVars.size();
	}

	// Return a successor label of weaving location corresponding to
	// the input 'end'.
	private static LabelNode getEndLabel(MethodNode methodNode,
			AbstractInsnNode instr) {

		if (instr.getNext() != null
				&& AsmHelper.skipVirualInsns(instr.getNext(), true) != null) {

			LabelNode branch = new LabelNode();
			methodNode.instructions.insert(instr, branch);

			JumpInsnNode jump = new JumpInsnNode(Opcodes.GOTO, branch);
			methodNode.instructions.insert(instr, jump);
			instr = jump;
		}

		// Create a label just after the 'GOTO' instruction.
		LabelNode label = new LabelNode();
		methodNode.instructions.insert(instr, label);
		return label;
	}

	// generate a try catch block node given the scope of the handler
	public static TryCatchBlockNode getTryCatchBlock(MethodNode methodNode,
			AbstractInsnNode start, AbstractInsnNode end) {

		InsnList ilst = methodNode.instructions;

		int new_start_offset = ilst.indexOf(start);
		int new_end_offset = ilst.indexOf(end);

		for (TryCatchBlockNode tcb : methodNode.tryCatchBlocks) {

			int start_offset = ilst.indexOf(tcb.start);
			int end_offset = ilst.indexOf(tcb.end);

			if (AsmHelper.offsetBefore(ilst, new_start_offset, start_offset)
					&& AsmHelper.offsetBefore(ilst, start_offset,
							new_end_offset)
					&& AsmHelper.offsetBefore(ilst, new_end_offset, end_offset)) {

				new_start_offset = start_offset;
			} else if (AsmHelper.offsetBefore(ilst, start_offset,
					new_start_offset)
					&& AsmHelper.offsetBefore(ilst, new_start_offset,
							end_offset)
					&& AsmHelper.offsetBefore(ilst, end_offset, new_end_offset)) {

				new_start_offset = end_offset;
			}
		}

		start = ilst.get(new_start_offset);
		end = ilst.get(new_end_offset);

		LabelNode startLabel = (LabelNode) start;
		LabelNode endLabel = getEndLabel(methodNode, end);

		return new TryCatchBlockNode(startLabel, endLabel, endLabel, null);
	}

	private static void insert(MethodNode methodNode,
			SCGenerator staticInfoHolder, PIResolver piResolver,
			WeavingInfo info, Snippet snippet, SnippetCode code, Shadow shadow,
			AbstractInsnNode loc) throws DynamicContextException {
		
		// exception handler will discard the stack and push the
		// exception object. Thus, before entering this snippet,
		// weaver must backup the stack and restore when exiting
		if (code.containsHandledException()
				&& info.stackNotEmpty(loc)) {

			InsnList backup = info.backupStack(loc,
					methodNode.maxLocals);
			InsnList restore = info.restoreStack(loc,
					methodNode.maxLocals);
			methodNode.maxLocals += info.getStackHeight(loc);

			methodNode.instructions.insertBefore(loc, backup);
			methodNode.instructions.insert(loc, restore);
		}

		WeavingCode wCode = new WeavingCode(info, methodNode,
				code, snippet, shadow, loc);
		wCode.transform(staticInfoHolder, piResolver, false);

		methodNode.instructions.insert(loc, wCode.getiList());
		methodNode.tryCatchBlocks.addAll(wCode.getTCBs());
	}

	public static void instrument(ClassNode classNode, MethodNode methodNode,
			Map<Snippet, List<Shadow>> snippetMarkings,
			List<SyntheticLocalVar> syntheticLocalVars,
			SCGenerator staticInfoHolder, PIResolver piResolver)
			throws DynamicContextException {

		WeavingInfo info = new WeavingInfo(classNode, methodNode,
				snippetMarkings);

		for (Snippet snippet : info.getSortedSnippets()) {
			List<Shadow> shadows = snippetMarkings.get(snippet);
			SnippetCode code = snippet.getCode();

			// skip snippet with empty code
			if (code == null) {
				continue;
			}

			// Instrument
			// For @Before, instrument the snippet just before the
			// entrance of a region.
			if (snippet.getAnnotationClass().equals(Before.class)) {
				for (Shadow shadow : shadows) {

					AbstractInsnNode loc = shadow.getWeavingRegion().getStart();
					insert(methodNode, staticInfoHolder, piResolver, info,
							snippet, code, shadow, loc);
				}
			}

			// For normal after(after returning), instrument the snippet
			// after each adjusted exit of a region.
			if (snippet.getAnnotationClass().equals(AfterReturning.class)
					|| snippet.getAnnotationClass().equals(After.class)) {
				for (Shadow shadow : shadows) {

					for (AbstractInsnNode loc : shadow.getWeavingRegion().getEnds()) {

						insert(methodNode, staticInfoHolder, piResolver, info,
								snippet, code, shadow, loc);
					}
				}
			}

			// For exceptional after(after throwing), wrap the region with
			// a try-finally clause. And append the snippet as an exception
			// handler.
			if (snippet.getAnnotationClass().equals(AfterThrowing.class)
					|| snippet.getAnnotationClass().equals(After.class)) {

				for (Shadow shadow : shadows) {
					
					WeavingRegion region = shadow.getWeavingRegion();
					// after-throwing inserts the snippet once, and marks
					// the start and the very end as the scope
					AbstractInsnNode loc = region.getAfterThrowEnd();

					WeavingCode wCode = new WeavingCode(info, methodNode, code,
							snippet, shadow, loc);
					wCode.transform(staticInfoHolder, piResolver, true);

					// Create a try-catch clause
					TryCatchBlockNode tcb = getTryCatchBlock(methodNode,
							region.getAfterThrowStart(), loc);

					methodNode.instructions.insert(tcb.handler,
							wCode.getiList());

					methodNode.tryCatchBlocks.add(tcb);
					methodNode.tryCatchBlocks.addAll(wCode.getTCBs());
				}
			}
		}

		static2Local(methodNode, syntheticLocalVars);
		AdvancedSorter.sort(methodNode);
	}

}
