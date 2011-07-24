package ch.usi.dag.disl.test.symbolic.symbol;

import stp.Expr;
import stp.VC;

public class VarSymbol implements Symbol {

	public String id;

	public VarSymbol(String id) {
		this.id = id;
	}

	@Override
	public Expr translate(VC vc) {
		return vc.varExpr(id, vc.bv32Type());		
	}
}
