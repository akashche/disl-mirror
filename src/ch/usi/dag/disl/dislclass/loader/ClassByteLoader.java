package ch.usi.dag.disl.dislclass.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import ch.usi.dag.disl.exception.InitException;

public abstract class ClassByteLoader {

	private static final String MANIFEST = "META-INF/MANIFEST.MF";

	public static final String INSTR_JAR = "dislinstr.jar";
	
	public static final String DISL_CLASSES = "disl.classes";
	public static final String DISL_CLASSES_DELIM = ":";
	
	// TODO ! jar support - add jar dependency
	// TODO ! jar support - create ant task for test compilation
	// TODO ! jar support - create processor manifest
	// TODO ! jar support - test
	
	public static List<InputStream> loadDiSLClasses()
			throws InitException {

		try {
		
			List<InputStream> result = loadClassesFromProperty();
			
			if(result == null) {
				result = loadClassesFromManifest();
			}
			
			return result;
		
		} catch (IOException e) {
			throw new InitException(e);
		}
	}

	private static List<InputStream> loadClassesFromProperty()
			throws IOException {
		
		String classesList = System.getProperty(DISL_CLASSES);
		
		if ( (classesList != null) && (! classesList.isEmpty()) ) {
			
			List<InputStream> dislClasses = new LinkedList<InputStream>();
		
			for (String fileName : classesList.split(DISL_CLASSES_DELIM)) {

				File file = new File(fileName);
				dislClasses.add(new FileInputStream(file));
			}

			return dislClasses;
		}
		
		return null;
	}
	
	private static List<InputStream> loadClassesFromManifest()
			throws IOException {

		ClassLoader cl = ClassByteLoader.class.getClassLoader();
		
		String classesList = getClassesListFromManifest(cl);
		
		if ( (classesList != null) && (! classesList.isEmpty()) ) {
			
			List<InputStream> dislClasses = new LinkedList<InputStream>();
		
			for (String fileName : classesList.split(DISL_CLASSES_DELIM)) {

				dislClasses.add(cl.getResourceAsStream(fileName));
			}

			return dislClasses;
		}
		
		return null;
	}
	
	private static String getClassesListFromManifest(ClassLoader classLoader)
			throws IOException {

		// get all manifests...		
		Enumeration<URL> resources = classLoader.getResources(MANIFEST);
		
		// and find ours...
		while (resources.hasMoreElements()) {

			Manifest manifest = 
				new Manifest(resources.nextElement().openStream());
			
			Attributes attrs = manifest.getMainAttributes();
			
			// contains disl classes
			if(attrs != null) {
				
				String dislClasses = attrs.getValue(DISL_CLASSES);
				
				if(dislClasses != null) {
					return dislClasses;
				}
			}
		}
		
		return null;
	}
}
