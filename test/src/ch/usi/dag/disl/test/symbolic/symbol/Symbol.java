package ch.usi.dag.disl.test.symbolic.symbol;

import stp.Expr;
import stp.VC;

public interface Symbol {
	
	public Expr translate(VC vc);
}
