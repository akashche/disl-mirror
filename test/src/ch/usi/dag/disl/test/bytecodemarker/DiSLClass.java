package ch.usi.dag.disl.test.bytecodemarker;

import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.marker.BytecodeMarker;
import ch.usi.dag.disl.staticcontext.BytecodeSC;

public class DiSLClass {
	
	@Before(marker = BytecodeMarker.class, param="aload, if_icmpne", scope = "TargetClass.main")
	public static void precondition(BytecodeSC ba) {
		System.out.println(ba.getBytecodeNumber());
	}
}
