#define _POSIX_C_SOURCE 200908L

#include <assert.h>
#include <stdlib.h>
#include <unistd.h>

#include <sys/uio.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/tcp.h>
#include <netdb.h>

#include "common.h"
#include "connection.h"


static void
__connection_init (struct connection * connection, const int sockfd) {
	connection->sockfd = sockfd;
	list_init (& connection->cp_link);

#ifdef DEBUG
	connection->sent_bytes = 0;
	connection->recv_bytes = 0;
#endif /* DEBUG */
}


/**
 * Creates a new connection to the remote server.
 */
struct connection * connection_open (struct addrinfo * addr) {
	//
	// Create a stream socket to the given address and connect to the server.
	// Upon connection, disable the Nagle algorithm to avoid delays on the
	// sender side and create a wrapper object for the connection.
	//
	int sockfd = socket(addr->ai_family, SOCK_STREAM, 0);
	check_std_error(sockfd, -1, "failed to create socket");

	int connect_result = connect(sockfd, addr->ai_addr, addr->ai_addrlen);
	check_std_error(connect_result, -1, "failed to connect to server");

	int tcp_nodelay = 1;
	int sso_result = setsockopt (
		sockfd, IPPROTO_TCP, TCP_NODELAY,
		&tcp_nodelay, sizeof (tcp_nodelay)
	);
	check_std_error(sso_result, -1, "failed to enable TCP_NODELAY");

	//

	struct connection * connection =
		(struct connection *) malloc (sizeof (struct connection));
	check_error (connection == NULL, "failed to allocate connection structure");

	__connection_init (connection, sockfd);
	return connection;
}


/**
 * Closes the connection and destroys the connection structure.
 */
void
connection_close (struct connection * connection) {
	assert (connection != NULL);

#if DEBUG
	fprintf (
		stderr, "socket %d: sent bytes %llu, recv bytes %llu\n",
		connection->sockfd, connection->sent_bytes, connection->recv_bytes
	);
#endif /* DEBUG */

	close (connection->sockfd);
	free (connection);
}

//

static void
__socket_send (const int sockfd, const void * buf, const ssize_t len) {
	unsigned char * buf_tail = (unsigned char *) buf;
	size_t remaining = len;

	while (remaining > 0) {
		int sent = send (sockfd, buf_tail, remaining, 0);
		check_std_error (sent, -1, "error sending data to server");

		remaining -= sent;
		buf_tail += sent;
	}
}


/**
 * Sends data into the given connection.
 */
void
connection_send (struct connection * connection, const void * buf, const ssize_t len) {
	assert (connection != NULL);
	assert (buf != NULL);
	assert (len >= 0);

	__socket_send (connection->sockfd, buf, len);

#ifdef DEBUG
	connection->sent_bytes += len;
#endif /* DEBUG*/
}


/**
 * Sends vectored data into the given connection.
 */
void
connection_send_iov (struct connection * connection, const struct iovec * iov, int iovcnt) {
	assert (connection != NULL);
	assert (iov != NULL);

	ssize_t written = writev (connection->sockfd, iov, iovcnt);
	check_std_error (written, -1, "error sending data to server");

#ifdef DEBUG
	connection->sent_bytes += written;
#endif /* DEBUG */
}

//

static void
__socket_recv (const int sockfd, void * buf, ssize_t len) {
	unsigned char * buf_tail = (unsigned char *) buf;
	ssize_t remaining = len;

	while (remaining > 0) {
		int received = recv (sockfd, buf_tail, remaining, 0);
		check_std_error(received, -1, "error receiving data from server");

		remaining -= received;
		buf_tail += received;
	}

	check_error (remaining < 0, "received more data than expected");
}


/**
 * Receives a predefined amount of data from the given connection.
 */
void
connection_recv (struct connection * connection, void * buf, const ssize_t len) {
	assert (connection != NULL);
	assert (buf != NULL);
	assert (len >= 0);

	__socket_recv (connection->sockfd, buf, len);

#ifdef DEBUG
	connection->recv_bytes += len;
#endif /* DEBUG */
}

/**
 * Receives vectored data from the given connection.
 */
void
connection_recv_iov (struct connection * connection, const struct iovec * iov, int iovcnt) {
	assert (connection != NULL);
	assert (iov != NULL);

	ssize_t read = readv (connection->sockfd, iov, iovcnt);
	check_std_error (read, -1, "error receiving data from server");

#ifdef DEBUG
	connection->recv_bytes += read;
#endif /* DEBUG */
}
