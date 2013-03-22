#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <sys/uio.h>

#include <jni.h>

#include "common.h"
#include "msgchannel.h"
#include "connection.h"


/**
 * Sends a message to the remote instrumentation server.
 */
void
message_send (struct connection * conn, struct message * msg) {
	assert (conn != NULL);
	assert (msg != NULL);

#ifdef DEBUG
	printf (
		"debug: sending message: flags %08x, control %d, code %d ... ", 
		msg->message_flags, msg->control_size, msg->classcode_size
	);
#endif

	jint ints [] = {
		htonl (msg->message_flags),
		htonl (msg->control_size),
		htonl (msg->classcode_size)
	};

	struct iovec iovs [] = {
		{ .iov_base = &ints [0], .iov_len = sizeof (ints) },
		{ .iov_base = (void *) msg->control, .iov_len = msg->control_size },
		{ .iov_base = (void *) msg->classcode, .iov_len = msg->classcode_size }
	};

	// TODO Handle the possibility of partial write.
	ssize_t sent = connection_send_iov (conn, &iovs [0], sizeof_array (iovs));
	assert (sent == (ssize_t) (sizeof (ints) + msg->control_size + msg->classcode_size));

#ifdef DEBUG
	printf ("done\n");
#endif
}


static inline unsigned char *
__alloc_buffer (size_t len) {
	//
	// Allocated a buffer with an extra (zeroed) byte, but only if the
	// requested buffer length is greater than zero. Return NULL otherwise.
	//
	if (len == 0) {
		return NULL;
	}

	//

	unsigned char * buf = (unsigned char *) malloc (len + 1);
	check_error (buf == NULL, "failed to allocate buffer");

	buf [len] = '\0';
	return buf;
}


/**
 * Receives a message from the remote instrumentation server.
 */
void
message_recv (struct connection * conn, struct message * msg) {
	assert (conn != NULL);
	assert (msg != NULL);

#ifdef DEBUG
	printf ("debug: receiving message: ");
#endif

	//
	// First, receive the flags, the control and class code sizes.
	// Second, receive the control and class code data.
	// The ordering of receive calls is determined by the protocol.
	//
	jint ints [3];
	connection_recv (conn, &ints [0], sizeof (ints));
	
	jint response_flags = ntohl (ints [0]);
	jint control_size = ntohl (ints [1]);
	jint classcode_size = ntohl (ints [2]);
	
	//
	
	unsigned char * control = __alloc_buffer (control_size);
	if (control_size > 0) {
		connection_recv (conn, control, control_size);
	}
	
	unsigned char * classcode = __alloc_buffer (classcode_size);
	if (classcode_size > 0) {
		connection_recv (conn, classcode, classcode_size);
	}

	//

	msg->message_flags = response_flags;
	msg->control_size = control_size;
	msg->classcode_size = classcode_size;
	msg->control = control;
	msg->classcode = classcode;

#ifdef DEBUG
	printf (
		"flags %08x, control %d, code %d ... done\n", 
		response_flags, control_size, classcode_size
	);
#endif
}
