package ch.usi.dag.disl.example.map.runtime;

import java.util.concurrent.atomic.AtomicLong;
import ch.usi.dag.jborat.runtime.DynamicBypass;

public class MemoryRuntime {

	private static boolean DEBUG = false;

	private static AtomicLong getfield, putfield, getstatic, putstatic,
	aaload, baload, caload, daload, faload, iaload, laload, saload,
	aastore, bastore, castore, dastore, fastore, iastore, lastore, sastore,
	newobject, newarray, anewarray, multianewarray, arraylength, monitorenter, monitorexit;

	static {
		boolean old = DynamicBypass.get();
		DynamicBypass.activate();
		try {
		getfield = new AtomicLong(0);
		putfield = new AtomicLong(0);
		getstatic = new AtomicLong(0);
		putstatic = new AtomicLong(0);

		aaload = new AtomicLong(0);
		baload = new AtomicLong(0);
		caload = new AtomicLong(0);
		daload = new AtomicLong(0);
		faload = new AtomicLong(0);
		iaload = new AtomicLong(0);
		laload = new AtomicLong(0);
		saload = new AtomicLong(0);

		aastore = new AtomicLong(0);
		bastore = new AtomicLong(0);
		castore = new AtomicLong(0);
		dastore = new AtomicLong(0);
		fastore = new AtomicLong(0);
		iastore = new AtomicLong(0);
		lastore = new AtomicLong(0);
		sastore = new AtomicLong(0);

		newobject = new AtomicLong(0);
		newarray = new AtomicLong(0);
		anewarray = new AtomicLong(0);
		multianewarray = new AtomicLong(0);
		arraylength = new AtomicLong(0);
		monitorenter = new AtomicLong(0);
		monitorexit = new AtomicLong(0);

		Thread shutdownHook = new Thread() {
			public void run () {
				boolean old = DynamicBypass.get();
				DynamicBypass.activate();
				try {
				System.err.println("[GETFIELD] " + getfield.get());
				System.err.println("[PUTFIELD] " + putfield.get());
				System.err.println("[GETSTATIC] " + getstatic.get());
				System.err.println("[PUTSTATIC] " + putstatic.get());

				System.err.println("[AALOAD] " + aaload.get());
				System.err.println("[BALOAD] " + baload.get());
				System.err.println("[CALOAD] " + caload.get());
				System.err.println("[DALOAD] " + daload.get());
				System.err.println("[FALOAD] " + faload.get());
				System.err.println("[IALOAD] " + iaload.get());
				System.err.println("[LALOAD] " + laload.get());
				System.err.println("[SALOAD] " + saload.get());

				System.err.println("[AASTORE] " + aastore.get());
				System.err.println("[BASTORE] " + bastore.get());
				System.err.println("[CASTORE] " + castore.get());
				System.err.println("[DASTORE] " + dastore.get());
				System.err.println("[FASTORE] " + fastore.get());
				System.err.println("[IASTORE] " + iastore.get());
				System.err.println("[LASTORE] " + lastore.get());
				System.err.println("[SASTORE] " + sastore.get());

				System.err.println("[NEW] " + newobject.get());
				System.err.println("[NEWARRAY] " + newarray.get());
				System.err.println("[ANEWARRAY] " + anewarray.get());
				System.err.println("[MULTIANEWARRAY] " + multianewarray.get());
				System.err.println("[ARRAYLENGTH] " + arraylength.get());
				System.err.println("[MONITORENTER] " + monitorenter.get());
				System.err.println("[MONITOREXIT] " + monitorexit.get());
				}finally {
					if(old)
						DynamicBypass.activate();
					else
				        DynamicBypass.deactivate();
				}
			}	
		};
		Runtime.getRuntime().addShutdownHook(shutdownHook);
		}finally {
			if(old)
				DynamicBypass.activate();
			else
		        DynamicBypass.deactivate();
		}
	} 


	public static void beforeGetfield(Object o, String fid) {
	//	DynamicBypass.activate();
		//System.out.println("*********** THIS IS THE RUNTIME *********" + DynamicBypass.get());
		if(DEBUG) {
			System.out.println("DiSL: Before GET The object " + o.toString());
			System.out.println("DiSL: Before GET The field name '" + fid + "'");
		}
//		boolean old = DynamicBypass.get();
//		DynamicBypass.activate();
//		try {
		getfield.incrementAndGet();
//		}finally{
//			if(old)
//				DynamicBypass.activate();
//			else
//				DynamicBypass.deactivate();
//		}
		
	//	DynamicBypass.deactivate();
	}

	public static void beforePutfield(Object o, String fid) {
		if(DEBUG) {
			System.out.println("DiSL: Before PUT The object " + o.toString());
			System.out.println("DiSL: Before PUT The field name '" + fid + "'");
		}
		putfield.incrementAndGet();
	}

	public static void beforeGetstatic(String fid) {
		if(DEBUG) {
			System.out.println("DiSL: Before GET STATIC '" + fid + "'");
		}
//		boolean old = DynamicBypass.get();
//		DynamicBypass.activate();
//		try {
		getstatic.incrementAndGet();
//		}finally{
//			if(old)
//				DynamicBypass.activate();
//			else
//				DynamicBypass.deactivate();
//		}
	}

	public static void beforePutstatic(String fid) {
		if(DEBUG) {
			System.out.println("DiSL: Before PUT STATIC '" + fid + "'");
		}
		putstatic.incrementAndGet();

	}

	public static void beforeAaload(Object[] o, int i) {
		if(DEBUG) {
			System.out.println("DiSL: INDEX " + i);
			System.out.println("DiSL: ARRAY " + o.toString());
		}
		aaload.incrementAndGet();

	}

	public static void beforeBaload(byte[] o, int i) {
		if(DEBUG) {
			System.out.println("DiSL: BEFORE Baload  " + o + " INDEX " + i);
		}
		baload.incrementAndGet();

	}

	public static void beforeCaload(char[] o, int i) {
		if(DEBUG) {
			System.out.println("DiSL : BEFORE CALOAD " + o.toString() + " INDEX " + i);
		}
		caload.incrementAndGet();

	}

	public static void beforeDaload(double[] o, int i) {
		if(DEBUG) {
			System.out.println("DiSL : BEFORE DALOAD " + o.toString() + " INDEX " + i);
		}
		daload.incrementAndGet();

	}

	public static void beforeFaload(float[] o, int i) {
		if(DEBUG) {
			System.out.println("DiSL : BEFORE FALOAD " + o.toString() + " INDEX " + i);
		}
		faload.incrementAndGet();

	}

	public static void beforeIaload(int[] o, int i) {
		if(DEBUG) {
			System.out.println("DiSL : BEFORE IALOAD  " + o.toString() + " INDEX " + i);
		}
		iaload.incrementAndGet();

	}

	public static void beforeLaload(long[] o, int i) {
		if(DEBUG) {
			System.out.println("DiSL : BEFORE LALOAD  " + o.toString() + " INDEX " + i);
		}
		laload.incrementAndGet();

	}

	public static void beforeSaload(short[] o, int i) {
		if(DEBUG) {
			System.out.println("DiSL : BEFORE SALOAD " + o + " INDEX " + i);
		}
		saload.incrementAndGet();

	}

	public static void beforeAastore(Object[] o, int i) {
		if(DEBUG) {
			System.out.println("DiSL : BEFORE AASTORE " + o + " INDEX " + i);
		}
		aastore.incrementAndGet();

	}

	public static void beforeBastore(byte[] o, int i) {
		if(DEBUG) {
			System.out.println("DiSL: BEFORE BSTORE  " + o + " INDEX " + i);
		}
		bastore.incrementAndGet();

	}

	public static void beforeCastore(char[] o, int i) {
		if(DEBUG) {
			System.out.println("DiSL : BEFORE CASTORE  " + o.toString() + " INDEX " + i);
		}
		castore.incrementAndGet();

	}

	public static void beforeDastore(double[] o, int i) {
		if(DEBUG) {
			System.out.println("DiSL : BEFORE DSTORE  " + o.toString() + " INDEX " + i);
		}
		dastore.incrementAndGet();

	}

	public static void beforeFastore(float[] o, int i) {
		if(DEBUG) {
			System.out.println("DiSL : BEFORE FSTORE  " + o.toString() + " INDEX " + i);
		}
		fastore.incrementAndGet();

	}

	public static void beforeIastore(int[] o, int i) {
		if(DEBUG) {
			System.out.println("DiSL : BEFORE ISTORE  " + o.toString() + " INDEX " + i);
		}
		iastore.incrementAndGet();

	}

	public static void beforeLastore(long[] o, int i) {
		if(DEBUG) {
			System.out.println("DiSL : BEFORE LSTORE  " + o.toString() + " INDEX " + i);
		}
		lastore.incrementAndGet();
	}

	public static void beforeSastore(short[] o, int i) {
		if(DEBUG) {
			System.out.println("DiSL : BEFORE SASTORE " + o + " idx " + i );
		}
		sastore.incrementAndGet();

	}

	//	   public static void afterNewobject(Object o) {
	public static void afterNewobject(Object o, String id) {
		newobject.incrementAndGet();
	}

	public static void afterNewarray(Object o) {
		if(DEBUG) {
			System.out.println("DiSL : After new array " + o.toString());
		}
		newarray.incrementAndGet();

	}

	public static void afterAnewarray(Object[] o) {
		if(DEBUG) {
			System.out.println("DiSL : After A new array" + o.toString());
		}
		anewarray.incrementAndGet();

	}

	public static void afterMultianewarray(Object o, int dims) {
		if(DEBUG) {
			System.out.println("DiSL : After A NEW MULTIARRAY " + o.toString());
			System.out.println("DiSL : DIMENSION of A NEW MULTIARRAY " + dims);
		}
		multianewarray.incrementAndGet();

	}

	public static void beforeArraylength(Object o) {
		if(DEBUG) {
			System.out.println("DiSL : Before ArrayLength " + o.toString());
		}
		arraylength.incrementAndGet();

	}

	public static void afterMonitorenter(Object o) {
		if(DEBUG) {
			System.out.println("DiSL: After MonitorEnter " + o.toString());
		}
		monitorenter.incrementAndGet();

	}

	public static void beforeMonitorexit(Object o) {
		if(DEBUG) {
			System.out.println("DiSL: Before MonitorExit on object " + o.toString());
		}
		monitorexit.incrementAndGet();

	}
}