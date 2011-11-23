package ch.usi.dag.disl.guardcontext;

public interface GuardContext {

	boolean invoke(Class<?> guardClass);
}
