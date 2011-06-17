package ch.usi.dag.disl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class CompilerStub {

	// thx: http://www.exampledepot.com/egs/java.io/File2ByteArray.html
	public byte [] readFileAsBytes(String fileName) throws IOException {
		
		// Create input stream
		File file = new File(fileName);
		InputStream is = new FileInputStream(file);
		byte[] bytes = null;
		
		try {
		
			// Get the size of the file
			long length = file.length();
	
			// You cannot create an array using a long type.
			// It needs to be an int type.
			// Before converting to an int type, check
			// to ensure that file is not larger than Integer.MAX_VALUE.
			if (length > Integer.MAX_VALUE) {
				
				throw new IOException(
						"File " + file.getName() + " is too large");
			}
	
			// Create the byte array to hold the data
			bytes = new byte[(int) length];
	
			// Read in the bytes
			int offset = 0;
			while (offset < bytes.length) {
				
				int numRead = is.read(bytes, offset, bytes.length - offset);

				// Input is expected but -1 (end of file) was returned
				if(numRead < 0) {
					throw new IOException("Could not completely read file "
							+ file.getName());
				}
				
				offset += numRead;
			}
			
		}
		finally {

			// Close the input stream...
			is.close();
		}

		// ... and return bytes
		return bytes;
	}
	
	byte [] compile(String fileName) throws IOException {

		// read class from file instead of compiling
		return readFileAsBytes(fileName);
	}
}
