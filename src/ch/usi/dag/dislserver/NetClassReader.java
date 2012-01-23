package ch.usi.dag.dislserver;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class NetClassReader {

	private final Socket socket;
	private final DataInputStream is;
	private final OutputStream os;

	public NetClassReader(Socket socket) throws IOException {
		
		this.socket = socket;

		is = new DataInputStream(socket.getInputStream());
		os = new BufferedOutputStream(socket.getOutputStream());
	}

	public ClassAsBytes readClassAsBytes() throws IOException {

		// protocol:
		// java int - class name length (cnl)
		// java int - code length (cl)
		// bytes[cnl] - class name
		// bytes[cl] - class code
		
		// TODO jb !
		//int nameLength = is.readInt();
		//int codeLength = is.readInt();
		
		int nameLength = readInt(is);
		int codeLength = readInt(is);

		// end of communication
		if(nameLength == 0 && codeLength == 0) {
			return null;
		}

		// allocate buffer for class reading
		byte[] name = new byte[nameLength];
		byte[] code = new byte[codeLength];

		// read class
		is.readFully(name);
		is.readFully(code);

		return new ClassAsBytes(name, code);
	}

	public void close() throws IOException {

		is.close();
		socket.close();
	}

	public void sendClassAsBytes(ClassAsBytes cab) throws IOException {

		// protocol:
		// java int - class name length (cnl)
		// java int - code length (cl)
		// bytes[cnl] - class name
		// bytes[cl] - class code
		
		// TODO jb !
		//os.write(cab.getName().length);
		//os.write(cab.getCode().length);
		
		writeInt(os, cab.getName().length);
		writeInt(os, cab.getCode().length);
		
		os.write(cab.getName());
		os.write(cab.getCode());
		os.flush();
	}
	
	// TODO jb ! remove all below ===
	
	private static int readInt(DataInputStream dis) throws IOException {
		
		byte[] data = new byte[4];
		dis.read(data);
		return byteArrayToInt(data);
	}
	
	private static void writeInt(OutputStream os, int value) throws IOException {
		
		os.write(intToByteArray(value));
	}
	
	private static int byteArrayToInt(byte[] b) {
		int value = 0;
		// REVERT THE VALUE OF THE PART CONTAINING THE INT
		byte[] mybuf = new byte[4];
		for (int i = 0; i < 4; i++) {
			mybuf[i] = b[3 - i];
		}

		for (int i = 0; i < 4; i++) {
			int shift = (4 - 1 - i) * 8;
			// value += (mybuf[i + offset] & 0x000000FF) << shift;
			value += (mybuf[i] & 0x000000FF) << shift;
		}
		return value;
	}

	private static byte[] intToByteArray(int value) {

		byte[] dest = new byte[4];

		for (int idx = 0; idx < 4; idx++) {
			dest[idx] = ((byte) (value >>> (idx * 8)));
		}

		return dest;
	}
}
