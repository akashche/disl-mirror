package ch.usi.dag.disl.test.symbolic.symbol;

import stp.Expr;
import stp.VC;

public class IConstValue implements Symbol {

	public int value;

	public IConstValue(int value) {
		this.value = value;
	}

	@Override
	public Expr translate(VC vc) {
		return vc.bv32ConstExprFromInt(value);
	}
}
