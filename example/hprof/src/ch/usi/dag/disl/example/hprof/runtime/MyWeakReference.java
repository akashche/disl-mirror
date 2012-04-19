package ch.usi.dag.disl.example.hprof.runtime;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;

public class MyWeakReference extends PhantomReference<Object>{

	private final String fullAllocSiteID;
	private final long size;

	public MyWeakReference(Object obj, String fullAllocSiteID, long size, ReferenceQueue<Object> refqueue) {
		super(obj,refqueue);
		this.fullAllocSiteID = fullAllocSiteID;
		this.size = size;
	}

	public String getFullAllocSiteID(){
		return fullAllocSiteID;
	}

	public long getSize() {
		return size;
	}
}
