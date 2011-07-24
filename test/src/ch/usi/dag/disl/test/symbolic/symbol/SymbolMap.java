package ch.usi.dag.disl.test.symbolic.symbol;

import java.util.HashMap;


@SuppressWarnings("serial")
public class SymbolMap extends HashMap<String, Symbol> {

	@Override
	public Symbol get(Object key) {
		Symbol symbol = super.get(key);
		return symbol == null ? new VarSymbol(key.toString()) : symbol;
	}
}
