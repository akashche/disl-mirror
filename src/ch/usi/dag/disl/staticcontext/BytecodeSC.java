package ch.usi.dag.disl.staticcontext;


public class BytecodeSC extends AbstractStaticContext {

	public int getBytecodeNumber() {
		
		return staticContextData.getMarkedRegion().getStart().getOpcode();
	}
}
