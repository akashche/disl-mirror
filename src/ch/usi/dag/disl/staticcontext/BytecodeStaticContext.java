package ch.usi.dag.disl.staticcontext;


public class BytecodeStaticContext extends AbstractStaticContext {

	public int getBytecodeNumber() {
		
		return staticContextData.getRegionStart().getOpcode();
	}
}
