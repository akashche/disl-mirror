#ifndef _CONNECTION_H_
#define _CONNECTION_H_

#include <sys/uio.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netdb.h>

#include "list.h"

struct connection {
	/** File descriptor of the connection socket. */
	int sockfd;

	/** Link in the connection pool list. */
	struct list cp_link;

#ifdef DEBUG
	/** Number of bytes sent over the connection. */
	unsigned long long sent_bytes;

	/** Number of bytes received over the connection. */
	unsigned long long recv_bytes;
#endif /* DEBUG */

};

struct connection * connection_open (struct addrinfo * addr);
void connection_close (struct connection * connection);

void connection_send (struct connection * connection, const void * buf, const ssize_t len);
void connection_send_iov (struct connection * connection, const struct iovec * iov, int iovcnt);
void connection_recv (struct connection * connection, void * buf, const ssize_t len);
void connection_recv_iov (struct connection * connection, const struct iovec * iov, int iovcnt);

#endif /* _CONNECTION_H_ */
