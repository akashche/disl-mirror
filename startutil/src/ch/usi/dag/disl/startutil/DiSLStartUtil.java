package ch.usi.dag.disl.startutil;

import java.io.File;
import java.io.FileInputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

public class DiSLStartUtil {

	private static final String DISL_PROPERTIES = "DiSL.properties";

	public static void main(String[] args) {

		try {

			// TODO create more general tool - ant task?
			// a) creates jar from DiSL classes + support classes
			//  - outputs jar file and within manifest with DiSL classes
			// b1) creates thread local jar using jborat util
			// b2) runs jborat + disl
			
			// gather all DiSL classes
			List<String> dislClasses = new LinkedList<String>();

			int i = 0;
			while (i < args.length) {

				dislClasses.add(args[i]);
				++i;
			}

			// parse thread local vars
			List<ThreadLocalVar> tlv = new LinkedList<ThreadLocalVar>();
			for (String dislClass : dislClasses) {

				File dcFile = new File(dislClass);

				tlv.addAll(ClassParser.usedTLV(dcFile));
			}

			// read properties file
			Properties properties = new Properties();
			properties.load(new FileInputStream(DISL_PROPERTIES));

			// TODO ! use extend thread utility

		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

}
