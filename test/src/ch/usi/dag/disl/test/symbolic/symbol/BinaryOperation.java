package ch.usi.dag.disl.test.symbolic.symbol;

import org.objectweb.asm.Opcodes;

import stp.Expr;
import stp.VC;

public class BinaryOperation implements Symbol {

	public Symbol left;
	public Symbol right;
	public int opcode;

	public BinaryOperation(int opcode, Symbol right, Symbol left) {
		this.left = left;
		this.right = right;
		this.opcode = opcode;
	}

	@Override
	public Expr translate(VC vc) {
		switch (opcode) {
		case Opcodes.ISUB:
			return vc.bvMinusExpr(32, left.translate(vc), right.translate(vc));
		case Opcodes.IF_ICMPLE:
			return vc.bvLeExpr(left.translate(vc), right.translate(vc));
		default:
			return null;
		}
	}
}
