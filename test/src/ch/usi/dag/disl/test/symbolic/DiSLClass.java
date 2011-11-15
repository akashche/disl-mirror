package ch.usi.dag.disl.test.symbolic;

import java.util.Stack;

import org.objectweb.asm.Opcodes;

import ch.usi.dag.disl.annotation.AfterReturning;
import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.annotation.SyntheticLocal;
import ch.usi.dag.disl.marker.BodyMarker;
import ch.usi.dag.disl.marker.BytecodeMarker;
import ch.usi.dag.disl.test.symbolic.symbol.BinaryOperation;
import ch.usi.dag.disl.test.symbolic.symbol.Constraint;
import ch.usi.dag.disl.test.symbolic.symbol.IConstValue;
import ch.usi.dag.disl.test.symbolic.symbol.SEAnalysis;
import ch.usi.dag.disl.test.symbolic.symbol.Symbol;
import ch.usi.dag.disl.test.symbolic.symbol.SymbolMap;

public class DiSLClass {

	@SyntheticLocal
	static SymbolMap symbols = new SymbolMap();

	@SyntheticLocal
	static Stack<Symbol> stackmodel = new Stack<Symbol>();

	@SyntheticLocal
	static Constraint constraints = new Constraint();

	@Before(marker = BytecodeMarker.class, param = "iconst_M1, iconst_0, iconst_1, "
			+ "iconst_2, iconst_3, iconst_4, iconst_5", scope = "TargetClass.print")
	public static void iconst(SEAnalysis ba) {
		stackmodel.push(new IConstValue(ba.getIConst()));
	}
	
	@Before(marker = BytecodeMarker.class, param = "iload", scope = "TargetClass.print")
	public static void iload(SEAnalysis ba) {
		stackmodel.push(symbols.get(ba.getID()));
	}

	@Before(marker = BytecodeMarker.class, param = "iinc", scope = "TargetClass.print")
	public static void iinc(SEAnalysis ba) {
		String id = ba.getID();
		symbols.put(id, new BinaryOperation(Opcodes.ISUB, new IConstValue(ba.getIConst()), 
				symbols.get(id)));
	}

	@Before(marker = BytecodeMarker.class, param = "if_icmple", scope = "TargetClass.print")
	public static void branch(SEAnalysis ba) {
		constraints.add(new BinaryOperation(ba.getBytecodeNumber(), stackmodel.pop(),
				stackmodel.pop()));
	}
	
	@AfterReturning(marker = BytecodeMarker.class, args = "if_icmple", scope = "TargetClass.print")
	public static void branch_fall(SEAnalysis ba) {
		constraints.fall();
	}
	
	@AfterReturning(marker = BodyMarker.class, scope = "TargetClass.print")
	public static void parse(SEAnalysis ba) {
		constraints.solve();
	}
}
