#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <netdb.h>
#include <unistd.h>
#include <netinet/tcp.h>

#include <jvmti.h>
#include <jni.h>

// has to be defined for jvmtihelper.h
#define ERR_PREFIX "DiSL-RE agent error: "

#include "../src-agent-c/jvmtihelper.h"
#include "../src-agent-c/comm.h"

// TODO remove when trunk merged
#define TRUE 1
#define FALSE 0

#include "messagetype.h"
#include "buffer.h"
#include "buffpack.h"

#include "dislreagent.h"

static const int ERR_SERVER = 10003;

// defaults - be sure that space in host_name is long enough
static const char * DEFAULT_HOST = "localhost";
static const char * DEFAULT_PORT = "11218";

// port and name of the instrumentation server
static char host_name[1024];
static char port_number[6]; // including final 0

static jvmtiEnv * jvmti_env;
static int jvm_started = FALSE;

// *** Protected by connection lock ***

static jrawMonitorID connection_lock;

// communication connection socket descriptor
// access must be protected by monitor
static int connection = 0;

// *** Protected by buff lock ***

static jrawMonitorID buff_lock;

// this number decides maximum number of threads writing
// also it determines memory consumption because buffers are left allocated
// BUFF_COUNT * MAX_BUFF_SIZE (defined in buffer.h) says max memory occupation
// cannot be static const :(
// note that if this number will not be sufficient, threads will start cycling
// in acquire_buff
#define BUFF_COUNT 512

// last used dispatch buffer - searching for free one starts from here
static volatile int buff_last_used = 0;

// array of disptach buffers
static buffer buffs[BUFF_COUNT];

// *** Protected by objectid lock ***

static jrawMonitorID objectid_lock;

// first available id for object taging
static jlong avail_object_tag = 1;

// ******************* Helper routines *******************

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

// ******************* Advanced buffer routines *******************

static jint acquire_buff() {

	const jint INVALID_ID = -1;

	jint buff_id = INVALID_ID;

	// bigger while could help with starvation
	//  - thread will be rescheduled faster
	//  - depends on lock implementation of course
	while(buff_id == INVALID_ID) {

		enter_critical_section(jvmti_env, buff_lock);
		{

			// find an available buffer

			jint try_id = buff_last_used; // we should find a buffer easier

			// end if we have valid id or we are on the starting position
			do {

				// get buffer
				if(buffs[try_id].available == TRUE) {

					buff_id = try_id;
					buffs[try_id].available = FALSE;
					buff_last_used = buff_id;
				}

				// shift to another buffer
				try_id = (try_id + 1) % BUFF_COUNT;
			}
			while(buff_id == INVALID_ID && try_id != buff_last_used);

		}
		exit_critical_section(jvmti_env, buff_lock);
	}

	return buff_id;
}

static void release_buff(jint buff_pos) {

	// no need for locking

	// clean buffer
	buffer_clean(&buffs[buff_pos]);

	// and made it available
	buffs[buff_pos].available = TRUE;
}

static void send_buffer(buffer * b) {

	// protect send
	enter_critical_section(jvmti_env, connection_lock);
	{
		// send data
		send_data(connection, b->buff, b->occupied);
	}
	exit_critical_section(jvmti_env, connection_lock);
}

static void send_buffer_schedule(buffer * buff) {

	// TODO
	send_buffer(buff);
}

// sends all buffered messages and this buffer
static void send_buffer_force(buffer * buff) {

	// TODO
	send_buffer(buff);
}

// ******************* Connection routines *******************

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

	// free host address info
	freeaddrinfo(addr);

	return sockfd;
}

static void close_connection(int conn) {

	// send close message
	jint buff_id = acquire_buff();

	buffer * buff = &buffs[buff_id];

	// msg id
	pack_int(buff, MSG_CLOSE);

	// force sending of messages in buffer
	send_buffer_force(buff);

	release_buff(buff_id);

	// close socket
	close(conn);
}

// ******************* Advanced packing routines *******************

static void pack_string_java(buffer * buff, jstring to_send, JNIEnv * jni_env) {

	// get string length
	jsize str_len = (*jni_env)->GetStringUTFLength(jni_env, to_send);

	// get string data as utf-8
	const char * str = (*jni_env)->GetStringUTFChars(jni_env, to_send, NULL);
	check_std_error(str == NULL, TRUE, "Cannot get string from java");

	// check if the size is sendable
	int size_fits = str_len < UINT16_MAX;
	check_std_error(size_fits, FALSE, "Java string is too big for sending");

	// send string
	pack_string_utf8(buff, str, str_len);

	// release string
	(*jni_env)->ReleaseStringUTFChars(jni_env, to_send, str);
}

static jlong get_object_id(jobject obj) {

	jlong obj_id;

	enter_critical_section(jvmti_env, objectid_lock);
	{

		jvmtiError error;

		// get object tag
		error = (*jvmti_env)->GetTag(jvmti_env, obj, &obj_id);
		check_jvmti_error(jvmti_env, error, "Cannot get object tag");

		// set object tag
		if(obj_id == 0) {

			obj_id = avail_object_tag;
			++avail_object_tag;

			// TODO add class id - note that class can miss the class id

			error = (*jvmti_env)->SetTag(jvmti_env, obj, obj_id);
			check_jvmti_error(jvmti_env, error, "Cannot set object tag");
		}

	}
	exit_critical_section(jvmti_env, objectid_lock);

	return obj_id;
}

static void pack_object(buffer * buff, jobject to_send) {

	pack_long(buff, get_object_id(to_send));
}

static void pack_class(buffer * buff, jclass to_send) {

	// TODO
	// class id is set for jclass on the same spot as for object
	// class id can have object id also

	// TODO
	// if class does not have id, you have to find it by name and class loader

	pack_int(buff, 1);
}

// ******************* analysis helper methods *******************

static jint analysis_start(jint analysis_method_id) {

	// TODO
	/*
	static __thread jint thread_id = 0;
	static __thread jint preferred_buffer;

	if(thread_id == 0) {
		thread_id = get_new_thread_id();
		acquire_buff()
	}
	else {
		acquire_buff(preferred_buffer)
	}
	*/

	// preferred_buffer = sid;

	// get session id - free buffer pos
	jint sid = acquire_buff();

	buffer * buff = &buffs[sid];

	// analysis msg
	pack_int(buff, MSG_ANALYZE);

	// method id
	pack_int(buff, analysis_method_id);

	// TODO
	// thread id
	//pack_int(buff, thread_id);

	return sid;
}

static void analysis_end(jint sid) {

	send_buffer_schedule(&buffs[sid]);

	release_buff(sid);
}

// TODO lock each callback using global lock

// ******************* CLASS LOAD callback *******************

void JNICALL jvmti_callback_class_file_load_hook( jvmtiEnv *jvmti_env,
		JNIEnv* jni_env, jclass class_being_redefined, jobject loader,
		const char* name, jobject protection_domain, jint class_data_len,
		const unsigned char* class_data, jint* new_class_data_len,
		unsigned char** new_class_data) {

	// TODO instrument analysis classes

	// *** send class info ***

	// send new class message
	jint buff_id = acquire_buff();

	buffer * buff = &buffs[buff_id];

	// retrieve class loader id

	jlong loader_id = 0;
	if(loader != NULL) { // bootstrap class loader has id 0 - invalid object id
		if(jvm_started) {
			loader_id = get_object_id(loader);
			// TODO test highest bit + update class loader number from reference
		}
		else {
			// TODO generate id
		}
	}

	// msg id
	pack_int(buff, MSG_NEW_CLASS);
	// class name
	pack_string_utf8(buff, name, strlen(name));
	// class loader id
	pack_long(buff, loader_id);
	// class code length
	pack_int(buff, class_data_len);
	// class code
	pack_bytes(buff, class_data, class_data_len);

	send_buffer_schedule(buff);

	release_buff(buff_id);
}

// ******************* OBJECT FREE callback *******************

void JNICALL jvmti_callback_class_object_free_hook(jvmtiEnv *jvmti_env, jlong tag) {

	// send obj free message
	jint buff_id = acquire_buff();

	buffer * buff = &buffs[buff_id];

	// msg id
	pack_int(buff, MSG_OBJ_FREE);
	// obj id
	pack_long(buff, tag);

	send_buffer_schedule(buff);

	release_buff(buff_id);
}

// ******************* START callback *******************

void JNICALL jvmti_callback_class_vm_start_hook(jvmtiEnv *jvmti_env, JNIEnv* jni_env) {

	jvm_started = TRUE;
}

// ******************* SHUTDOWN callback *******************

void JNICALL jvmti_callback_class_vm_death_hook(jvmtiEnv *jvmti_env, JNIEnv* jni_env) {

	close_connection(connection);

	int i; // C99 needed for initialization in cycle
	for(i = 0; i < BUFF_COUNT; ++i) {
		buffer_free(&buffs[i]);
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
	cap.can_generate_all_class_hook_events = TRUE;

	// tagging objects
	cap.can_tag_objects = TRUE;

	// tagged objects free
	cap.can_generate_object_free_events = TRUE;

	error = (*jvmti_env)->AddCapabilities(jvmti_env, &cap);
	check_jvmti_error(jvmti_env, error,
			"Unable to get necessary JVMTI capabilities.");

	// adding callbacks
	jvmtiEventCallbacks callbacks;
	(void) memset(&callbacks, 0, sizeof(callbacks));

	callbacks.ClassFileLoadHook = &jvmti_callback_class_file_load_hook;
	callbacks.VMStart = &jvmti_callback_class_vm_start_hook;
	callbacks.VMDeath = &jvmti_callback_class_vm_death_hook;
	callbacks.ObjectFree = &jvmti_callback_class_object_free_hook;

	(*jvmti_env)->SetEventCallbacks(jvmti_env, &callbacks,
			(jint) sizeof(callbacks));

	error = (*jvmti_env)->SetEventNotificationMode(jvmti_env, JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL);
	check_jvmti_error(jvmti_env, error, "Cannot set class load hook");

	error = (*jvmti_env)->SetEventNotificationMode(jvmti_env, JVMTI_ENABLE, JVMTI_EVENT_VM_START, NULL);
	check_jvmti_error(jvmti_env, error, "Cannot set jvm start hook");

	error = (*jvmti_env)->SetEventNotificationMode(jvmti_env, JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH, NULL);
	check_jvmti_error(jvmti_env, error, "Cannot set jvm death hook");

	error = (*jvmti_env)->SetEventNotificationMode(jvmti_env, JVMTI_ENABLE, JVMTI_EVENT_OBJECT_FREE, NULL);
	check_jvmti_error(jvmti_env, error, "Cannot set object free hook");

	error = (*jvmti_env)->CreateRawMonitor(jvmti_env, "connection socket", &connection_lock);
	check_jvmti_error(jvmti_env, error, "Cannot create raw monitor");

	error = (*jvmti_env)->CreateRawMonitor(jvmti_env, "buffers", &buff_lock);
	check_jvmti_error(jvmti_env, error, "Cannot create raw monitor");

	error = (*jvmti_env)->CreateRawMonitor(jvmti_env, "object tags", &objectid_lock);
	check_jvmti_error(jvmti_env, error, "Cannot create raw monitor");

	// read options (port/hostname)
	parse_agent_options(options);

	connection = open_connection();

	int i; // C99 needed :)
	for(i = 0; i < BUFF_COUNT; ++i) {
		buffer_alloc(&buffs[i]);
	}

	return 0;
}

// ******************* REDispatch methods *******************

JNIEXPORT jint JNICALL Java_ch_usi_dag_dislre_REDispatch_analysisStart
  (JNIEnv * jni_env, jclass this_class, jint analysis_method_id) {

	return analysis_start(analysis_method_id);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_analysisEnd
  (JNIEnv * jni_env, jclass this_class, jint sid) {

	analysis_end(sid);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendBoolean
  (JNIEnv * jni_env, jclass this_class, jint sid, jboolean to_send) {

	pack_boolean(&buffs[sid], to_send);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendByte
  (JNIEnv * jni_env, jclass this_class, jint sid, jbyte to_send) {

	pack_byte(&buffs[sid], to_send);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendChar
  (JNIEnv * jni_env, jclass this_class, jint sid, jchar to_send) {

	pack_char(&buffs[sid], to_send);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendShort
  (JNIEnv * jni_env, jclass this_class, jint sid, jshort to_send) {

	pack_short(&buffs[sid], to_send);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendInt
  (JNIEnv * jni_env, jclass this_class, jint sid, jint to_send) {

	pack_int(&buffs[sid], to_send);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendLong
  (JNIEnv * jni_env, jclass this_class, jint sid, jlong to_send) {

	pack_long(&buffs[sid], to_send);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendFloatAsInt
  (JNIEnv * jni_env, jclass this_class, jint sid, jint to_send) {

	pack_int(&buffs[sid], to_send);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendDoubleAsLong
  (JNIEnv * jni_env, jclass this_class, jint sid, jlong to_send) {

	pack_long(&buffs[sid], to_send);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendString
  (JNIEnv * jni_env, jclass this_class, jint sid, jstring to_send) {

	pack_string_java(&buffs[sid], to_send, jni_env);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendObject
  (JNIEnv * jni_env, jclass this_class, jint sid, jobject to_send) {

	pack_object(&buffs[sid], to_send);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendClass
  (JNIEnv * jni_env, jclass this_class, jint sid, jclass to_send) {

	pack_class(&buffs[sid], to_send);
}
