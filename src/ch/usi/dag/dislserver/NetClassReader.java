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
		
		int nameLength = is.readInt();
		int codeLength = is.readInt();

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
		
		os.write(cab.getName().length);
		os.write(cab.getCode().length);
		os.write(cab.getName());
		os.write(cab.getCode());
		os.flush();
	}
}
