package ch.usi.dag.disl.example.fieldsImmutabilityAnalysis.runtime;

import java.lang.ref.WeakReference;

public class MyWeakReference<T> extends WeakReference<T> {
	public final String objectID;

	MyWeakReference(T referent, String objectID) { 
		super(referent); 
		this.objectID = objectID;
	}
}
