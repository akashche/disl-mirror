package ch.usi.dag.disl.example.fieldsImmutabilityAnalysis;

import ch.usi.dag.disl.staticcontext.AbstractStaticContext;

public class MyStaticContext extends AbstractStaticContext {

	public boolean isInTheConstructor() {

		return (staticContextData.getMethodNode().name.equals("<init>") && !staticContextData.getMethodNode().name.equals("java/lang/Object")) ? true : false;

	}
	
	public boolean isInTheStaticInitializer() {
		return (staticContextData.getMethodNode().name.equals("<clinit>") ? false : true);
	}
}
