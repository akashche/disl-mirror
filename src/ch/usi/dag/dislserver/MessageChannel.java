package ch.usi.dag.dislserver;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;


public class MessageChannel {

	private final DataInputStream is;
	private final DataOutputStream os;

	//

	public MessageChannel(Socket socket) throws IOException {
		socket.setTcpNoDelay (true);

		is = new DataInputStream (
			new BufferedInputStream (socket.getInputStream ())
		);

		os = new DataOutputStream (
			new BufferedOutputStream (socket.getOutputStream ())
		);
	}

	//

	public Message recvMessage () throws IOException {
		// protocol:
		// java int - request flags
		// java int - control string length (ctl)
		// java int - class code length (ccl)
		// bytes[ctl] - control string (contains class name)
		// bytes[ccl] - class code

		final int flags = is.readInt ();
		final int controlLength = is.readInt ();
		final int classCodeLength = is.readInt ();

		final byte [] control = new byte [controlLength];
		is.readFully (control);

		final byte [] classCode = new byte [classCodeLength];
		is.readFully (classCode);

		return new Message (flags, control, classCode);
	}


	public void sendMessage (Message message) throws IOException {
		// protocol:
		// java int - response flags
		// java int - control string (ctl)
		// java int - class code length (ccl)
		// bytes[ctl] - control string
		// bytes[ccl] - class code

		os.writeInt (message.getFlags ());
		os.writeInt (message.getControl ().length);
		os.writeInt (message.getClassCode ().length);

		os.write (message.getControl ());
		os.write (message.getClassCode ());

		os.flush();
	}


	public void close () {
		try {
			is.close ();
			os.close ();
		} catch (final IOException e) {
			// not much we can do about it
		}
	}

}
