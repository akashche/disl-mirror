#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <netdb.h>
#include <unistd.h>
#include <netinet/tcp.h>

#include <jvmti.h>
#include <jni.h>

#include "dislreagent.h"

static const int ERR_JVMTI = 10001;
static const int ERR_COMM = 10002;
static const int ERR_SERVER = 10003;

static const char * ERR_PREFIX = "ERROR in DiSL-RE agent: ";

static const int TRUE = 1;
static const int FALSE = 0;

// defaults - be sure that space in host_name is long enough
static const char * DEFAULT_HOST = "localhost";
static const char * DEFAULT_PORT = "11218";

// port and name of the instrumentation server
static char host_name[1024];
static char port_number[6]; // including final 0

static jvmtiEnv * jvmti_env;
static jrawMonitorID global_lock;

// communication connection socket descriptor
// access must be protected by monitor
static int connection = 0;

// Messages - should be in sync with java server

// closing connection
static const jint MSG_CLOSE = 0;

// TODO unify in header - helper
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

// TODO unify in header - communication
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

// sends data over network
static void send_msg_type(int sockfd, jint msg_type) {

	jint network_msg_type = htonl(msg_type);
	send_data(sockfd, &network_msg_type, sizeof(jint));
}

static int open_connection() {

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

	return sockfd;
}

static void close_connection(int conn) {

	send_msg_type(conn, MSG_CLOSE);

	// close socket
	close(conn);
}

// ******************* MSG types *******************

// TODO unify in header - instrumentation
// ******************* instrumentation routines

typedef struct {
	jint control_size;
	jint classcode_size;
	const unsigned char * control;
	const unsigned char * classcode;
} class_as_bytes;

static class_as_bytes create_class_as_bytes(const unsigned char * control,
		jint control_size, const unsigned char * classcode,
		jint classcode_size) {

	class_as_bytes result;

	// control + size
	result.control_size = control_size;

	// contract: (if control_size <= 0) pointer may be copied (stolen)
	if(control != NULL && control_size > 0) {

		// without ending 0
		unsigned char * buffcn = (unsigned char *) malloc(control_size);
		memcpy(buffcn, control, control_size);
		result.control = buffcn;
	}
	else {

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
	}
	else {

		result.classcode = classcode;
		result.classcode_size = abs(classcode_size);
	}

	return result;
}

static void free_class_as_bytes(class_as_bytes * cab) {

	if(cab->control != NULL) {

		// cast because of const
		free((void *) cab->control);
		cab->control = NULL;
		cab->control_size = 0;
	}

	if(cab->classcode != NULL) {

		// cast because of const
		free((void *) cab->classcode);
		cab->classcode = NULL;
		cab->classcode_size = 0;
	}
}

// sends class over network
static void send_instr(int sockfd, class_as_bytes * class_to_send) {

	// send control and code size first and then data

	// convert to java representation
	jint nctls = htonl(class_to_send->control_size);
	send_data(sockfd, &nctls, sizeof(jint));

	// convert to java representation
	jint nccs = htonl(class_to_send->classcode_size);
	send_data(sockfd, &nccs, sizeof(jint));

	send_data(sockfd, class_to_send->control, class_to_send->control_size);

	send_data(sockfd, class_to_send->classcode, class_to_send->classcode_size);
}

// receives class from network
class_as_bytes rcv_instr(int sockfd) {

	// receive control and code size first and then data

	// *** receive control size - jint
	jint nctls;
	rcv_data(sockfd, &nctls, sizeof(jint));

	// convert from java representation
	jint control_size = ntohl(nctls);

	// *** receive class code size - jint
	jint nccs;
	rcv_data(sockfd, &nccs, sizeof(jint));

	// convert from java representation
	jint classcode_size = ntohl(nccs);

	// *** receive control string
	// +1 - ending 0 - useful when printed - normally error msgs here
	unsigned char * control = (unsigned char *) malloc(control_size + 1);

	rcv_data(sockfd, control, control_size);

	// terminate string
	control[control_size] = '\0';

	// *** receive class code
	unsigned char * classcode = (unsigned char *) malloc(classcode_size);

	rcv_data(sockfd, classcode, classcode_size);

	// negative length - create_message adopts pointers
	return create_class_as_bytes(control, -control_size, classcode, -classcode_size);
}

// instruments remotely
static class_as_bytes instrument(const char * classname,
		const unsigned char * classcode, jint classcode_size) {

	// crate class data
	class_as_bytes cas = create_class_as_bytes((const unsigned char *)classname,
			strlen(classname), classcode, classcode_size);

	send_instr(connection, &cas);

	class_as_bytes result = rcv_instr(connection);

	return result;
}

// TODO lock each callback using global lock

// ******************* CLASS LOAD callback *******************

static void JNICALL jvmti_callback_class_file_load_hook( jvmtiEnv *jvmti_env,
		JNIEnv* jni_env, jclass class_being_redefined, jobject loader,
		const char* name, jobject protection_domain, jint class_data_len,
		const unsigned char* class_data, jint* new_class_data_len,
		unsigned char** new_class_data) {

	// TODO remove
	return;

	// TODO
	// if java.lang.object || analysis
	// send proper msg type

	// TODO send class to instr + manage result

	// ask the server to instrument
	class_as_bytes instrclass = instrument(name, class_data, class_data_len);

	// error on the server
	if (instrclass.control_size > 0) {

		// classname contains the error message

		fprintf(stderr, "%sError occurred in the remote instrumentation server\n",
				ERR_PREFIX);
		fprintf(stderr, "   Reason: %s\n", instrclass.control);
		exit(ERR_SERVER);
	}

	// instrumented class recieved (0 - means no instrumentation done)
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

	close_connection(connection);
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

	connection = open_connection();

	return 0;
}

// ******************* REDispatch methods *******************

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_analyse
  (JNIEnv * jni_env, jclass this_class, jint objID, jint methodID, jobjectArray args) {

	// TODO create send method for each basic type
	printf("I'm here\n");
}

