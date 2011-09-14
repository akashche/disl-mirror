package ch.usi.dag.disl.test.jarrewriter;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import ch.usi.dag.disl.DiSL;

/**
 * Tool to rewrite a jar, with DiSL
 */
public class JarRewriter {
    
    private static final String CLASS_FILE_EXT = ".class";
	private final DiSL disl;

    /**
     * @param instr Defines the Instrumentation used for rewriting classes
     * @throws Exception 
     */
    public JarRewriter() throws Exception {
        this.disl = new DiSL();
        disl.initialize();
    }

	/**
	 * Rewrites every class in a jar
	 * 
	 * @param jarInFileName
	 *            Input jar
	 * @param jarOutFileName
	 *            Output jar
	 */
	public void rewrite(String jarInFileName, String jarOutFileName) {
		
		ZipOutputStream jarOut = null;
		JarFile jarInFile = null;
		try {

			// open jars
			jarOut = new ZipOutputStream(new FileOutputStream(jarOutFileName));
			jarInFile = new JarFile(jarInFileName);
			
			Enumeration<JarEntry> jiEntries = jarInFile.entries();

			while (jiEntries.hasMoreElements()) {

				ZipEntry ze = (ZipEntry) jiEntries.nextElement();
				String entryName = ze.getName();
				InputStream classIS = jarInFile.getInputStream(ze);

				// something else then class
				if (! entryName.endsWith(CLASS_FILE_EXT)) {
					addEntry(jarOut, entryName, classIS);
					continue;
				}
				
				// get class name from entry name
				// first replace path delim
				String classname = entryName.replace('/', '.');
				// cut .class at the end
				classname = classname.substring(0, 
						entryName.lastIndexOf(CLASS_FILE_EXT));
				
				// TODO why ??
				if (classname.startsWith("[")) {
					addEntry(jarOut, entryName, classIS);
					continue;
				}

				try {

					// instrument it
					byte[] instrumentedBytes = disl.instrument(classIS);
					
					if(instrumentedBytes != null) {

						// put instrumented into jar
						addEntry(jarOut, entryName, instrumentedBytes);
					}
					else {
						
						// put original into jar
						addEntry(jarOut, entryName, classIS);
					}
				}
				catch (Exception e) {
					// catch exception, print it, and try another one
					e.printStackTrace();
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			
			try {
			
				if(jarOut != null) {
					jarOut.flush();
					jarOut.close();
				}
				
				if(jarInFile != null) {
					jarInFile.close();
				}
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	private void addEntry(ZipOutputStream zos, String entryName, byte[] bytes)
			throws IOException {
		addEntry(zos, entryName, new ByteArrayInputStream(bytes));
	}

	private void addEntry(ZipOutputStream zos, String entryName, InputStream is)
			throws IOException {
		
		ZipEntry nze = new ZipEntry(entryName);
		zos.putNextEntry(nze);

		final int BUFFER_LENGTH = 8192;
		byte[] buf = new byte[BUFFER_LENGTH];
		
		int bytesRead = is.read(buf);
		while (bytesRead != -1) {
			zos.write(buf, 0, bytesRead);
			bytesRead = is.read(buf);
		}
		
		zos.closeEntry();
	}

	public static void main(String args[]) {

		if (args.length != 2)
			usage();

		try {
			JarRewriter jr = new JarRewriter();
			jr.rewrite(args[1], args[2]);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static void usage() {
		System.out.println("Usage: JarRewriter <injar> <outjar>\n\tinstrumentation: a canonical classname of an implementation of Instrumentation\n\tinjar: input jarfile\n\toutjar: output jarfile");
		System.exit(1);
	}

}
