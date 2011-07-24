package ch.usi.dag.disl.test.symbolic.symbol;

import java.util.LinkedList;


import stp.Expr;
import stp.VC;

@SuppressWarnings("serial")
public class Constraint extends LinkedList<Symbol> {
	public LinkedList<Boolean> path = new LinkedList<Boolean>();
	public boolean flag = false;

	public void fall() {
		path.add(false);
		flag = false;
	}

	@Override
	public boolean add(Symbol e) {
		if (flag) {
			path.add(true);
		}

		flag = true;
		return super.add(e);
	}

	public void solve(VC vc, int size, boolean reverse) {
		Expr[] constraints = new Expr[size];
		vc.push();

		for (int i = 0; i < size; i++) {
			Expr constraint = get(i).translate(vc);
			constraints[i] = path.get(i) ? constraint : vc.notExpr(constraint);
		}

		if (size > 1) {
			if (reverse) {
				constraints[size - 1] = vc.notExpr(constraints[size - 1]);
			}

			vc.query(vc.notExpr(vc.andExprN(constraints)));
		} else {
			vc.query(reverse ? constraints[0] : vc.notExpr(constraints[0]));
		}

		vc.pop();
	}

	public void solve() {
		// Unhandle branch
		if (flag) {
			path.add(true);
		}

		// Validation
		int size = size();

		if (size == 0) {
			System.out.println("SE: Path contains no branch!");
			return;
		}

		if (path.size() != size) {
			System.out.println("SE: An error occur when generating the path!");
			return;
		}
		
		VC vc = new VC();

		VC.setFlags('n');
		VC.setFlags('d');
		VC.setFlags('p');

		System.out.println("++++++++++++++++++++++++++++++");
		solve(vc, size, false);

		for (int i = size; i > 0; i--) {
			System.out.println("++++++++++++++++++++++++++++++");
			solve(vc, i, true);
		}
	}
}
