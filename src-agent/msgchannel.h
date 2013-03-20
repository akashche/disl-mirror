#ifndef _MSGCHANNEL_H_
#define _MSGCHANNEL_H_

#include <jni.h>

#include "connection.h"


struct message {
	jint message_flags;
	jint control_size;
	jint classcode_size;
	const unsigned char * control;
	const unsigned char * classcode;
};


struct message create_message (
	jint message_flags,
	const unsigned char * control, jint control_size,
	const unsigned char * classcode, jint classcode_size
);

void free_message(struct message * msg);

void send_message (struct connection * conn, struct message * msg);
struct message recv_message (struct connection * conn);

#endif /* _MSGCHANNEL_H_ */
