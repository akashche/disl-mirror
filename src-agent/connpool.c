#define _POSIX_C_SOURCE 200908L

#include <stdio.h>

#include "common.h"

#include "list.h"
#include "connection.h"
#include "connpool.h"


void
connection_pool_init (struct connection_pool * cp, struct addrinfo * endpoint) {
	assert (cp != NULL);
	assert (endpoint != NULL);

	cp->connections_count = 0;
	list_init (&cp->free_connections);
	list_init (&cp->busy_connections);

	cp->endpoint = endpoint;
	cp->after_open_hook = NULL;
	cp->before_close_hook = NULL;
}


void
connection_pool_set_after_open_hook (struct connection_pool * cp, connection_hook_fn after_open_fn) {
	assert (cp != NULL);

	cp->after_open_hook = after_open_fn;
}


void
connection_pool_set_before_close_hook (struct connection_pool * cp, connection_hook_fn before_close_fn) {
	assert (cp != NULL);

	cp->before_close_hook = before_close_fn;
}


/**
 * Acquires a connection from a pool of available connections. Creates a new
 * connection if none is currently available. This operation is thread-unsafe.
 */
struct connection *
connection_pool_get_connection (struct connection_pool * cp) {
	assert (cp != NULL);

	//
	// Grab the first available connection and return. If there is no connection
	// available, create a new one and add it to the busy connection list.
	//
	if (!list_is_empty (&cp->free_connections)) {
		struct list * item = list_remove_after (&cp->free_connections);
		list_insert_after (item, &cp->busy_connections);
		return list_item (item, struct connection, cp_link);

	} else {
		struct connection * connection = connection_open (cp->endpoint);
		if (cp->after_open_hook != NULL) {
			cp->after_open_hook (connection);
		}

		list_insert_after (&connection->cp_link, &cp->busy_connections);
		cp->connections_count++;
#ifdef DEBUG
		printf ("[new connection, %d in total] ", cp->connections_count);
#endif
		return connection;
	}
}


/**
 * Releases the given connection and puts it back to the pool of available
 * connections. This operation is thread-unsafe.
 */
void
connection_pool_put_connection (
	struct connection_pool * cp, struct connection * connection
) {
	assert (cp != NULL);
	assert (connection != NULL);

	//
	// Move the connection from the list of busy connections to
	// the list of available connections.
	//
	struct list * item = list_remove (&connection->cp_link);
	list_insert_after (item, &cp->free_connections);
}

//

static void
__connection_destructor (struct list * item, void * data) {
	struct connection * connection = list_item (item, struct connection, cp_link);

	struct connection_pool * cp = (struct connection_pool *) data;
	if (cp->before_close_hook != NULL) {
		cp->before_close_hook (connection);
	}

	connection_close (connection);
	cp->connections_count--;
}


/**
 * Closes all connections in the given connection pool. The connection pool
 * may be reused to open new connections to the host it was initialized for.
 */
void
connection_pool_close (struct connection_pool * cp) {
	assert (cp != NULL);

#ifdef DEBUG
	printf (
		"debug: connection pool for %s: max connections %d\n",
		cp->endpoint->ai_canonname, cp->connections_count
	);
#endif

	list_destroy (&cp->free_connections, __connection_destructor, (void *) cp);
	if (!list_is_empty (&cp->busy_connections)) {
		fprintf (stderr, "warning: closing %d active connections", cp->connections_count);
		list_destroy (&cp->busy_connections, __connection_destructor, (void *) cp);
	}
}
