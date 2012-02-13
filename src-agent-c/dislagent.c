#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <netdb.h>
#include <unistd.h>
#include <netinet/tcp.h>

#include <jvmti.h>
#include <jni.h>

#include "dislagent.h"

static const int ERR_JVMTI = 10001;
static const int ERR_COMM = 10002;
static const int ERR_SERVER = 10003;

static const char * ERR_PREFIX = "ERROR in DiSL agent: ";

static const int TRUE = 1;
static const int FALSE = 0;

// defaults - be sure that space in host_name is long enough
static const char * DEFAULT_HOST = "localhost";
static const char * DEFAULT_PORT = "11217";

typedef struct {
	jint classname_size;
	jint classcode_size;
	const char * classname;
	const unsigned char * classcode;
} class_as_bytes;

// linked list to hold socket file descriptor
// access must be protected by monitor
struct strc_connection_item {
	int sockfd;
	volatile int available;
	struct strc_connection_item *next;
};

typedef struct strc_connection_item connection_item;

// port and name of the instrumentation server
static char host_name[1024];
static char port_number[6]; // including final 0

static jvmtiEnv * jvmti_env;
static jrawMonitorID global_lock;

// we are using multiple connections for parallelization
// pros: there is no need for synchronization in this client :)
// cons: gets ugly with strong parallelization

// the first element on the list of connections
// modifications should be protected by critical section
static connection_item * conn_list = NULL;

// ******************* Helper routines *******************

/*
 * Check error routine - reporting on one place
 */
static void check_std_error(int retval, int errorval,
		const char *str) {

	if (retval == errorval) {

		static const int BUFFSIZE = 1024;

		char msgbuf[BUFFSIZE];

		snprintf(msgbuf, BUFFSIZE, "%s%s", ERR_PREFIX, str);

		perror(msgbuf);

		exit(ERR_COMM);
	}
}

/*
 * Every JVMTI interface returns an error code, which should be checked
 *   to avoid any cascading errors down the line.
 *   The interface GetErrorName() returns the actual enumeration constant
 *   name, making the error messages much easier to understand.
 */
static void check_jvmti_error(jvmtiEnv *jvmti, jvmtiError errnum,
		const char *str) {

	if (errnum != JVMTI_ERROR_NONE) {
		char *errnum_str;

		errnum_str = NULL;
		(void) (*jvmti)->GetErrorName(jvmti, errnum, &errnum_str);

		fprintf(stderr, "%sJVMTI: %d(%s): %s\n", ERR_PREFIX, errnum,
				(errnum_str == NULL ? "Unknown" : errnum_str),
				(str == NULL ? "" : str));

		exit(ERR_JVMTI);
	}
}

/*
 * Enter a critical section by doing a JVMTI Raw Monitor Enter
 */
static void enter_critical_section(jvmtiEnv *jvmti, jrawMonitorID lock_id) {

	jvmtiError error;

	error = (*jvmti)->RawMonitorEnter(jvmti, lock_id);
	check_jvmti_error(jvmti, error, "Cannot enter with raw monitor");
}

/*
 * Exit a critical section by doing a JVMTI Raw Monitor Exit
 */
static void exit_critical_section(jvmtiEnv *jvmti, jrawMonitorID lock_id) {

	jvmtiError error;

	error = (*jvmti)->RawMonitorExit(jvmti, lock_id);
	check_jvmti_error(jvmti, error, "Cannot exit with raw monitor");
}

static class_as_bytes create_class_as_bytes(const char * classname,
		jint classname_size, const unsigned char * classcode,
		jint classcode_size) {

	class_as_bytes result;

	result.classname_size = classname_size;

	// class name + size
	// contract: (if classname_size <= 0) pointer may be copied (stolen)
	if(classname != NULL && classname_size > 0) {

		char * buffcn = (char *) malloc(classname_size + 1); // +1 - ending 0
		memcpy(buffcn, classname, classname_size + 1);
		result.classname = buffcn;
	}
	else {

		result.classname = classname;
		result.classname_size = abs(classname_size);
	}

	// class code + size
	result.classcode_size = classcode_size;

	// contract: (if classcode_size <= 0) pointer may be copied (stolen)
	if(classcode != NULL && classcode_size > 0) {

		unsigned char * buffcc = (unsigned char *) malloc(classcode_size);
		memcpy(buffcc, classcode, classcode_size);
		result.classcode = buffcc;
	}
	else {

		result.classcode = classcode;
		result.classcode_size = abs(classcode_size);
	}

	return result;
}

static void free_class_as_bytes(class_as_bytes * cab) {

	if(cab->classname != NULL) {

		// cast because of const
		free((void *) cab->classname);
		cab->classname = NULL;
		cab->classname_size = 0;
	}

	if(cab->classcode != NULL) {

		// cast because of const
		free((void *) cab->classcode);
		cab->classcode = NULL;
		cab->classcode_size = 0;
	}
}

static void parse_agent_options(char *options) {

	static const char PORT_DELIM = ':';

	// assign defaults
	strcpy(host_name, DEFAULT_HOST);
	strcpy(port_number, DEFAULT_PORT);

	// no options found
	if (options == NULL) {
		return;
	}

	char * port_start = strchr(options, PORT_DELIM);

	// process port number
	if(port_start != NULL) {

		// replace PORT_DELIM with end of the string (0)
		port_start[0] = '\0';

		// move one char forward to locate port number
		++port_start;

		// convert number
		int fitsP = strlen(port_start) < sizeof(port_number);
		check_std_error(fitsP, FALSE, "Port number is too long");

		strcpy(port_number, port_start);
	}

	// check if host_name is big enough
	int fitsH = strlen(options) < sizeof(host_name);
	check_std_error(fitsH, FALSE, "Host name is too long");

	strcpy(host_name, options);
}

// ******************* Communication routines *******************

// sends data over network
static void send_data(int sockfd, const void * data, int data_len) {

	int sent = 0;

	while (sent != data_len) {

		int res = send(sockfd, ((unsigned char *)data) + sent,
				(data_len - sent), 0);
		check_std_error(res, -1, "Error while sending data to server");
		sent += res;
	}
}

// receives data from network
static void rcv_data(int sockfd, void * data, int data_len) {

	int received = 0;

	while (received != data_len) {

		int res = recv(sockfd, ((unsigned char *)data) + received,
				(data_len - received), 0);
		check_std_error(res, -1, "Error while receiving data from server");

		received += res;
	}
}

// sends class over network
static void send_class(connection_item * conn, class_as_bytes * class_to_send) {

	// send name and code size first and then data

	int sockfd = conn->sockfd;

	// convert to java representation
	jint ncns = htonl(class_to_send->classname_size);
	send_data(sockfd, &ncns, sizeof(jint));

	// convert to java representation
	jint nccs = htonl(class_to_send->classcode_size);
	send_data(sockfd, &nccs, sizeof(jint));

	send_data(sockfd, class_to_send->classname, class_to_send->classname_size);

	send_data(sockfd, class_to_send->classcode, class_to_send->classcode_size);
}

// receives class from network
class_as_bytes rcv_class(connection_item * conn) {

	// receive name and code size first and then data

	int sockfd = conn->sockfd;

	// *** receive class name size - jint
	jint ncns;
	rcv_data(sockfd, &ncns, sizeof(jint));

	// convert from java representation
	jint classname_size = ntohl(ncns);

	// *** receive class code size - jint
	jint nccs;
	rcv_data(sockfd, &nccs, sizeof(jint));

	// convert from java representation
	jint classcode_size = ntohl(nccs);

	// *** no transformation done
	if(classname_size == 0) {
		return create_class_as_bytes(NULL, 0, NULL, 0);
	}

	// *** receive class name
	char * classname = (char *) malloc(classname_size + 1); // +1 - ending 0

	rcv_data(sockfd, classname, classname_size);

	// terminate string
	classname[classname_size] = '\0';

	// *** error on the server
	if (classcode_size == 0) {

		// classname contains the error message

		fprintf(stderr, "%sError occurred in the remote instrumentation server\n",
				ERR_PREFIX);
		fprintf(stderr, "   Reason: %s\n", classname);
		exit(ERR_SERVER);
	}

	unsigned char * classcode = (unsigned char *) malloc(classcode_size);

	rcv_data(sockfd, classcode, classcode_size);

	// 0 length - create_class_as_bytes adopts pointers
	return create_class_as_bytes(classname, -classname_size,
			classcode, -classcode_size);
}

static connection_item * open_connection() {

	// get host address
	struct addrinfo * addr;
	int gai_res = getaddrinfo(host_name, port_number, NULL, &addr);
	check_std_error(gai_res == 0, FALSE, gai_strerror(gai_res));

	// create stream socket
	int sockfd = socket(addr->ai_family, SOCK_STREAM, 0);
	check_std_error(sockfd, -1, "Cannot create socket");

	// connect to server
	int conn_res = connect(sockfd, addr->ai_addr, addr->ai_addrlen);
	check_std_error(conn_res, -1, "Cannot connect to server");

	// disable Nagle algorithm
	// http://www.techrepublic.com/article/tcpip-options-for-high-performance-data-transmission/1050878
	int flag = 1;
	int set_res = setsockopt(sockfd, IPPROTO_TCP, TCP_NODELAY, &flag,
			sizeof(int));
	check_std_error(set_res, -1, "Cannot set TCP_NODELAY");

	// free host address info
	freeaddrinfo(addr);

	// allocate new connection information
	connection_item * conn = (connection_item *) malloc(sizeof(connection_item));
	conn->available = TRUE;
	conn->sockfd = sockfd;
	conn->next = NULL;

	return conn;
}

static void close_connection(connection_item * conn) {

	// prepare close message - could be done more efficiently (this is nicer)
	// close message has zeros as lengths
	class_as_bytes close_msg = create_class_as_bytes(NULL, 0, NULL, 0);

	// send close message
	send_class(conn, &close_msg);

	// nothing was allocated but for completeness
	free_class_as_bytes(&close_msg);

	// close socket
	close(conn->sockfd);

	// free connection space
	free(conn);
}

// get an available connection, create one if no one is available
static connection_item * acquire_connection() {

	connection_item * curr;

	// the connection list requires access using critical section
	enter_critical_section(jvmti_env, global_lock);
	{
		curr = conn_list;

		while (curr != NULL) {

			if (curr->available == TRUE) {
				break;
			}

			curr = curr->next;
		}

		if (curr == NULL) {

			// create new connection
			curr = open_connection();

			// add it at the beginning of the connection list
			curr->next = conn_list;
			conn_list = curr;
		}

		curr->available = FALSE;
	}
	exit_critical_section(jvmti_env, global_lock);

	return curr;
}

// make the socket available again
static void release_connection(connection_item * conn) {

	// the connection list requires access using critical section
	// BUT :), release can be done without it
	//enter_critical_section(jvmti_env, global_lock);
	{
		// make connection available
		conn->available = TRUE;
	}
	//exit_critical_section(jvmti_env, global_lock);
}

// instruments remotely
static class_as_bytes instrument_class(const char * classname,
		const unsigned char * classcode, jint classcode_size) {

	// get available connection
	connection_item * conn = acquire_connection();

	// crate class data
	class_as_bytes cas = create_class_as_bytes(classname, strlen(classname),
			classcode, classcode_size);

	send_class(conn, &cas);

	class_as_bytes result = rcv_class(conn);

	release_connection(conn);

	return result;
}

// ******************* CLASS LOAD callback *******************

static void JNICALL jvmti_callback_class_file_load_hook( jvmtiEnv *jvmti_env,
		JNIEnv* jni_env, jclass class_being_redefined, jobject loader,
		const char* name, jobject protection_domain, jint class_data_len,
		const unsigned char* class_data, jint* new_class_data_len,
		unsigned char** new_class_data) {

	// ask the server to instrument
	class_as_bytes instrclass = instrument_class(name, class_data, class_data_len);

	// valid class recieved
	if(instrclass.classcode_size > 0) {

		// give to JVM the instrumented class
		unsigned char *new_class_space;

		// let JVMTI to allocate the mem for the new class
		jvmtiError err = (*jvmti_env)->Allocate(jvmti_env, (jlong)instrclass.classcode_size, &new_class_space);
		check_jvmti_error(jvmti_env, err, "Cannot allocate memory for the instrumented class");

		memcpy(new_class_space, instrclass.classcode, instrclass.classcode_size);

		// set the newly instrumented class + len
		*(new_class_data_len) = instrclass.classcode_size;
		*(new_class_data) = new_class_space;

		// free memory
		free_class_as_bytes(&instrclass);
	}

}

// ******************* SHUTDOWN callback *******************

static void JNICALL jvmti_callback_class_vm_death_hook(jvmtiEnv *jvmti_env, JNIEnv* jni_env) {

	connection_item * cnode = conn_list;

	// will be deallocated in the while cycle
	conn_list = NULL;

	// close all connections
	while(cnode != NULL) {

		// prepare for closing
		connection_item * connToClose = cnode;

		// advance first - pointer will be invalid after close
		cnode = cnode->next;

		// close connection
		close_connection(connToClose);
	}
}

// ******************* JVMTI entry method *******************

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {

	jvmti_env = NULL;

	jint res = (*jvm)->GetEnv(jvm, (void **) &jvmti_env, JVMTI_VERSION_1_0);

	if (res != JNI_OK || jvmti_env == NULL) {
		/* This means that the VM was unable to obtain this version of the
		 *   JVMTI interface, this is a fatal error.
		 */
		fprintf(stderr, "%sUnable to access JVMTI Version 1 (0x%x),"
				" is your J2SE a 1.5 or newer version?"
				" JNIEnv's GetEnv() returned %d\n", ERR_PREFIX, JVMTI_VERSION_1,
				res);

		exit(ERR_JVMTI);
	}

	jvmtiError error;

	// adding hooks
	jvmtiCapabilities cap;
	memset(&cap, 0, sizeof(cap));

	// class hook
	cap.can_generate_all_class_hook_events = 1;

	// timer
	cap.can_get_current_thread_cpu_time = 1;

	error = (*jvmti_env)->AddCapabilities(jvmti_env, &cap);
	check_jvmti_error(jvmti_env, error,
			"Unable to get necessary JVMTI capabilities.");

	// adding callbacks
	jvmtiEventCallbacks callbacks;
	(void) memset(&callbacks, 0, sizeof(callbacks));

	callbacks.ClassFileLoadHook = &jvmti_callback_class_file_load_hook;
	callbacks.VMDeath = &jvmti_callback_class_vm_death_hook;

	(*jvmti_env)->SetEventCallbacks(jvmti_env, &callbacks,
			(jint) sizeof(callbacks));

	error = (*jvmti_env)->SetEventNotificationMode(jvmti_env, JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL);
	check_jvmti_error(jvmti_env, error, "Cannot set class load hook");

	error = (*jvmti_env)->SetEventNotificationMode(jvmti_env, JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH, NULL);
	check_jvmti_error(jvmti_env, error, "Cannot create jvm death hook");

	error = (*jvmti_env)->CreateRawMonitor(jvmti_env, "agent data", &global_lock);
	check_jvmti_error(jvmti_env, error, "Cannot create raw monitor");

	// read options (port/hostname)
	parse_agent_options(options);

	return 0;
}
