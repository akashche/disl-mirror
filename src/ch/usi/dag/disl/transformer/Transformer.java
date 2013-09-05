package ch.usi.dag.disl.transformer;

public interface Transformer {

	byte[] transform(byte[] classfileBuffer) throws Exception;
	
	boolean propagateUninstrumentedClasses();
}
