#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "common.h"
#include "msgchannel.h"
#include "connection.h"


struct message
create_message (
	jint message_flags,
	const unsigned char * control, jint control_size,
	const unsigned char * classcode, jint classcode_size
) {
	struct message result;

	result.message_flags = message_flags;

	// control + size
	result.control_size = control_size;

	// contract: (if control_size <= 0) pointer may be copied (stolen)
	if (control != NULL && control_size > 0) {
		// without ending 0
		unsigned char * buffcn = (unsigned char *) malloc(control_size);
		memcpy(buffcn, control, control_size);
		result.control = buffcn;
	} else {
		result.control = control;
		result.control_size = abs(control_size);
	}

	// class code + size
	result.classcode_size = classcode_size;

	// contract: (if classcode_size <= 0) pointer may be copied (stolen)
	if(classcode != NULL && classcode_size > 0) {
		unsigned char * buffcc = (unsigned char *) malloc(classcode_size);
		memcpy(buffcc, classcode, classcode_size);
		result.classcode = buffcc;
	} else {
		result.classcode = classcode;
		result.classcode_size = abs(classcode_size);
	}

	return result;
}


void
free_message(struct message * msg) {
	if(msg->control != NULL) {
		// cast because of const
		free((void *) msg->control);
		msg->control = NULL;
		msg->control_size = 0;
	}

	if(msg->classcode != NULL) {
		// cast because of const
		free((void *) msg->classcode);
		msg->classcode = NULL;
		msg->classcode_size = 0;
	}
}


/**
 * Sends a request message to the remote instrumentation server.
 */
void
send_message (struct connection * conn, struct message * msg) {
#ifdef DEBUG
	printf (
		"Sending - control: %d, code: %d ... ", 
		msg->control_size, msg->classcode_size
	);
#endif

	jint ints [3];
	ints [0] = htonl (msg->message_flags);
	ints [1] = htonl (msg->control_size);
	ints [2] = htonl (msg->classcode_size);

	struct iovec iovs [3];
	iovs [0].iov_base = &ints [0];
	iovs [0].iov_len = sizeof (ints);
	iovs [1].iov_base = (void *) msg->control;
	iovs [1].iov_len = msg->control_size;
	iovs [2].iov_base = (void *) msg->classcode;
	iovs [2].iov_len = msg->classcode_size;

	connection_send_iov (conn, &iovs [0], sizeof_array (iovs));

#ifdef DEBUG
	printf("done\n");
#endif
}


static inline unsigned char *
__alloc_bytes (size_t len) {
	unsigned char * buf = (unsigned char *) malloc (len + 1);
	check_error (buf == NULL, "failed to allocate receive buffer");
	buf [len] = '\0';
	return buf;
}


/**
 * Receives a response message from the remote instrumentation server.
 */
struct message
recv_message (struct connection * conn) {
#ifdef DEBUG
	printf("Receiving ");
#endif

	//
	// First, receive the flags, the control and class code sizes.
	// Second, receive the control and class code data.
	// The ordering of receive calls is determined by the protocol.
	//
	jint ints [3];
	connection_recv (conn, &ints [0], sizeof (ints));
	
	//
	
	jint response_flags = ntohl (ints [0]);
	jint control_size = ntohl (ints [1]);
	jint classcode_size = ntohl (ints [2]);
	
	unsigned char * control = __alloc_bytes (control_size);
	unsigned char * classcode = __alloc_bytes (classcode_size);

	//

	struct iovec iovs [2];
	iovs [0].iov_base = control;
	iovs [0].iov_len = control_size;
	iovs [1].iov_base = classcode;
	iovs [1].iov_len = classcode_size;

	connection_recv_iov (conn, &iovs [0], sizeof_array (iovs));

#ifdef DEBUG
	printf("- control: %d, code: %d ... done\n", control_size, classcode_size);
#endif

	// negative length - create_message adopts pointers
	return create_message(response_flags, control, -control_size, classcode, -classcode_size);
}
