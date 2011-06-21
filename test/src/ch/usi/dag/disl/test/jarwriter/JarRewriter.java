package ch.usi.dag.disl.test.jarwriter;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import ch.usi.dag.jborat.agent.Instrumentation;

/**
 * Tool to rewrite a jar, with an {@link Instrumentation}
 * Instrumentation should be thread-safe since the rewriting will probably be parallelized ;-)
 * @author pmoret
 *
 */
public class JarRewriter {
    
    private final Instrumentation instrumentation;

    /**
     * @param instr Defines the Instrumentation used for rewriting classes, it is final.
     */
    public JarRewriter(Instrumentation instr) {
        this.instrumentation = instr;
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
				InputStream jarIn = jarInFile.getInputStream(ze);

				// something else then class
				if (! entryName.endsWith(".class")) {
					addEntry(jarOut, entryName, jarIn);
					continue;
				}
				
				// get class name from entry name
				// first replace path delim
				String classname = entryName.replace('/', '.');
				// cut .class at the end
				classname = classname.substring(0, entryName.lastIndexOf(".class"));
				
				// TODO why ??
				if (classname.startsWith("[")) {
					addEntry(jarOut, entryName, jarIn);
					continue;
				}

				try {

					// read the class
					
					// FIXME: may not read it whole - length is estimated number 
					int length = jarIn.available();
					byte[] originalCode = new byte[length];
					int bytesRead = 0;
					while (bytesRead < length) {
						bytesRead += jarIn.read(originalCode, bytesRead,
								length - bytesRead);
					}
					
					// crate ASM class node
					ClassReader cr = new ClassReader(originalCode);
					ClassNode classNode = new ClassNode();
					cr.accept(classNode, 0);
					
					// instrument it
					instrumentation.instrument(classNode);
					
					ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
					classNode.accept(cw);

					byte[] instrumentedBytes = cw.toByteArray();
					
					// put it into jar
					addEntry(jarOut, entryName, instrumentedBytes);
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

		if (args.length != 3)
			usage();

		Instrumentation instr;

		try {
			instr = (Instrumentation) Class.forName(args[0]).newInstance();
			JarRewriter jr = new JarRewriter(instr);
			jr.rewrite(args[1], args[2]);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static void usage() {
		System.out.println("Usage: JarRewriter <instrumentation> <injar> <outjar>\n\tinstrumentation: a canonical classname of an implementation of Instrumentation\n\tinjar: input jarfile\n\toutjar: output jarfile");
		System.exit(1);
	}

}
