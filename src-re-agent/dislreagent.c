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
// cannot require other locks while holding this

static jrawMonitorID connection_lock;

// communication connection socket descriptor
// access must be protected by monitor
static int connection = 0;

// *** Protected by buff lock ***
// cannot require other locks while holding this

static jrawMonitorID buff_lock;

// last used dispatch buffer - searching for free one starts from here
static volatile int buff_last_used = 0;

// this number decides maximum number of threads sending data
// also it determines memory consumption because buffers are left allocated
// BUFF_COUNT * MAX_BUFF_SIZE (defined in buffer.h) says max memory occupation
// cannot be static const :(
// note that if this number will not be sufficient, threads will start cycling
// in acquire_buff
#define BUFF_COUNT 512

// array of disptach buffers
static buffer buffs[BUFF_COUNT];

// *** Protected by tagging lock ***
// can require other locks while holding this

static jrawMonitorID tagging_lock;

// first available id for object taging
static jlong avail_object_id = 1;
static jint avail_class_id = 1;

// *** Thread locals ***

static __thread jlong thread_id = 0;

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

	const int BUFF_INVALID_ID = -1;

	jint buff_id = BUFF_INVALID_ID;

	enter_critical_section(jvmti_env, buff_lock);
	{
		// find an available buffer
		jint try_id = buff_last_used;

		// this can cycle "really long" if all buffers are taken
		// BUFF_COUNT should be set to "high number"
		//  - help: consider how many threads should have locked buffer
		//          and over-dimension it
		while(buff_id == BUFF_INVALID_ID) {

			try_id = (try_id + 1) % BUFF_COUNT;

			if(buffs[try_id].available == TRUE) {

				buffs[try_id].available = FALSE;
				buff_id = try_id;
				buff_last_used = try_id;
			}
		}

	}
	exit_critical_section(jvmti_env, buff_lock);

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

// ******************* Net reference get/set routines *******************

// should be in sync with NetReference on the server

// format of net reference looks like this
// HIGHEST (1 bit spec, 23 bits class id, 40 bits object id)
// bit field not used because there is no guarantee of alignment

static const u_int8_t OBJECT_ID_POS = 0;
static const u_int8_t CLASS_ID_POS = 40;
static const u_int8_t SPEC_POS = 63;

static const u_int64_t OBJECT_ID_MASK = 0xFFFFFFFFFF;
static const u_int64_t CLASS_ID_MASK = 0x7FFFFF;
static const u_int64_t SPEC_MASK = 0x1;

// get bits from "from" with pattern "bit_mask" lowest bit starting on position
// "low_start" (from 0)
static inline u_int64_t get_bits(u_int64_t from, u_int64_t bit_mask,
		u_int8_t low_start) {

	// shift it
	u_int64_t bits_shifted = from >> low_start;

	// mask it
	return bits_shifted & bit_mask;
}

// set bits "bits" to "to" with pattern "bit_mask" lowest bit starting on
// position "low_start" (from 0)
static inline void set_bits(u_int64_t * to, u_int64_t bits,
		u_int64_t bit_mask, u_int8_t low_start) {

	// mask it
	u_int64_t bits_len = bits & bit_mask;
	// move it to position
	u_int64_t bits_pos = bits_len << low_start;
	// set
	*to |= bits_pos;
}

static inline jlong net_ref_get_object_id(jlong net_ref) {

	return get_bits(net_ref, OBJECT_ID_MASK, OBJECT_ID_POS);
}

static inline jint net_ref_get_class_id(jlong net_ref) {

	return get_bits(net_ref, CLASS_ID_MASK, CLASS_ID_POS);
}

static inline unsigned char net_ref_get_spec(jlong net_ref) {

	return get_bits(net_ref, SPEC_MASK, SPEC_POS);
}

static inline void net_ref_set_object_id(jlong * net_ref, jlong object_id) {

	set_bits((u_int64_t *)net_ref, object_id, OBJECT_ID_MASK, OBJECT_ID_POS);
}

static inline void net_ref_set_class_id(jlong * net_ref, jint class_id) {

	set_bits((u_int64_t *)net_ref, class_id, CLASS_ID_MASK, CLASS_ID_POS);
}

static inline void net_ref_set_spec(jlong * net_ref, unsigned char spec) {

	set_bits((u_int64_t *)net_ref, spec, SPEC_MASK, SPEC_POS);
}

// ******************* Net reference routines *******************

// TODO comment idea

static jclass get_class_for_object(jobject obj, JNIEnv * jni_env) {

	return (*jni_env)->GetObjectClass(jni_env, obj);
}

static int object_is_class(jobject obj, JNIEnv * jni_env) {

	// TODO isn't there better way?

	jvmtiError error = (*jvmti_env)->GetClassSignature(jvmti_env, obj, NULL, NULL);

	if(error != JVMTI_ERROR_NONE) {
		// object is not class
		return FALSE;
	}

	return TRUE;
}

// forward declaration
static jlong get_net_reference(jobject obj, JNIEnv * jni_env);

// this method can be called even if object tagging is not supported jet
static jlong get_class_loader_net_ref(jobject loader, JNIEnv * jni_env) {

	if(loader == NULL) { // net reference for NULL is 0
		return 0;
	}

	if(! jvm_started) {

		// jvm didn't started jet
		// we cannot use tagging - use backup solution

		// TODO create weak reference + generate id
		return 0;
	}

	// normal class loader id handling

	// TODO tagging_lock has to be used around this
	// we cannot use different lock because we can be called with or without
	// tagging lock but we require tagging lock in get_net_reference
	// assume we use class_loader_lock here
	// t1 calls get_class_loader_net_ref and gets class_loader_lock
	// t2 calls directly get_net_reference and gets tagging_lock
	// t1 waits in get_net_reference for tagging_lock
	// t2 waits in get_class_loader_net_ref
	//    (called from _set_net_reference_for_class) for class_loader_lock

	jlong loader_id = get_net_reference(loader, jni_env);

	// if spec is not set, check generated ids
	if(net_ref_get_spec(loader_id) == 0) {

		// TODO check generated ids

		// set spec flag
		net_ref_set_spec(&loader_id, 1);

		// TODO set tag
	}

	return loader_id;
}

// do not call me unless you know what you are doing
// does not perform tagging
static jlong _get_net_reference(jobject obj) {

	jlong net_ref;

	jvmtiError error = (*jvmti_env)->GetTag(jvmti_env, obj, &net_ref);
	check_jvmti_error(jvmti_env, error, "Cannot get object tag");

	return net_ref;
}

// do not call me unless you know what you are doing
// should be called only with tagging_lock
// does not increment any counter - just sets the values
static jlong _set_net_reference(jobject obj, JNIEnv * jni_env,
		jlong object_id, jint class_id, unsigned char spec) {

	jlong net_ref = 0;

	net_ref_set_object_id(&net_ref, object_id);
	net_ref_set_class_id(&net_ref, class_id);
	net_ref_set_spec(&net_ref, spec);

	jvmtiError error = (*jvmti_env)->SetTag(jvmti_env, obj, net_ref);
	check_jvmti_error(jvmti_env, error, "Cannot set object tag");

	return net_ref;
}

// do not call me unless you know what you are doing
static void _send_class_info(jlong class_net_ref, char * class_sig,
		char * class_gen, jlong class_loader_net_ref,
		jlong super_class_net_ref) {

	// class gen can be NULL, we have to handle it
	if(class_gen == NULL) {
		class_gen = ""; // send empty string
	}

	// send class info message
	jint buff_id = acquire_buff();

	buffer * buff = &buffs[buff_id];

	// msg id
	pack_int(buff, MSG_CLASS_INFO);
	// class id
	pack_int(buff, net_ref_get_class_id(class_net_ref));
	// class signature
	pack_string_utf8(buff, class_sig, strlen(class_sig));
	// class generic string
	pack_string_utf8(buff, class_gen, strlen(class_gen));
	// class loader id
	pack_long(buff, class_loader_net_ref);
	// super class id
	pack_int(buff, net_ref_get_class_id(super_class_net_ref));

	// send message
	send_buffer_schedule(buff);

	release_buff(buff_id);
}

// do not call me unless you know what you are doing
// should be called only with tagging_lock
static jlong _set_net_reference_for_class(jclass klass, JNIEnv * jni_env) {

	// manage references
	// http://docs.oracle.com/javase/6/docs/platform/jvmti/jvmti.html#refs
	static const jint ADD_REFS = 16;
	jint res = (*jni_env)->PushLocalFrame(jni_env, ADD_REFS);
	check_std_error(res == 0, FALSE, "Cannot allocate more references");

	// *** set net reference for class ***

	// TODO if you do one more getObjectClass then you (maybe) get another class
	// they should share same id
	//  - maybe solve this on server
	//  - you have to because Class<?> can be send first

	// assign new net reference - set spec to 1 (binding send over network)
	jlong net_ref = _set_net_reference(klass, jni_env, avail_object_id,
			avail_class_id, 1);

	// increment object id counter
	++avail_object_id;

	// increment class id counter
	++avail_class_id;

	// *** send class info over network ***

	jvmtiError error;

	// resolve descriptor + generic
	char * class_sig;
	char * class_gen;
	error = (*jvmti_env)->GetClassSignature(jvmti_env, klass, &class_sig,
			&class_gen);
	check_jvmti_error(jvmti_env, error, "Cannot get class signature");

	// resolve class loader...
	jobject class_loader;
	error = (*jvmti_env)->GetClassLoader(jvmti_env, klass, &class_loader);
	check_jvmti_error(jvmti_env, error, "Cannot get class loader");
	// ... + class loader id
	jlong class_loader_net_ref = get_class_loader_net_ref(class_loader, jni_env);

	// resolve super class...
	jclass super_class = (*jni_env)->GetSuperclass(jni_env, klass);
	// ... + super class id
	jlong super_class_net_ref = get_net_reference(super_class, jni_env);

	// send class info over network
	_send_class_info(net_ref, class_sig, class_gen, class_loader_net_ref,
			super_class_net_ref);

	// deallocate memory
	error = (*jvmti_env)->Deallocate(jvmti_env, (unsigned char *)class_sig);
	check_jvmti_error(jvmti_env, error, "Cannot deallocate memory");
	error = (*jvmti_env)->Deallocate(jvmti_env, (unsigned char *)class_gen);
	check_jvmti_error(jvmti_env, error, "Cannot deallocate memory");

	// manage references - see function top
	(*jni_env)->PopLocalFrame(jni_env, NULL);

	return net_ref;
}

// do not call me unless you know what you are doing
// should be called only with tagging_lock
static jint _get_class_id_for_class(jclass klass, JNIEnv * jni_env) {

	jlong class_net_ref = _get_net_reference(klass);

	if(class_net_ref == 0) {
		class_net_ref = _set_net_reference_for_class(klass, jni_env);
	}

	return net_ref_get_class_id(class_net_ref);
}

// do not call me unless you know what you are doing
// should be called only with tagging_lock
static jint _get_class_id_for_object(jobject obj, JNIEnv * jni_env) {

	// get class of this object
	jclass klass = get_class_for_object(obj, jni_env);

	// get class id of this class
	return _get_class_id_for_class(klass, jni_env);
}

// do not call me unless you know what you are doing
// should be called only with tagging_lock
static jlong _set_net_reference_for_object(jobject obj, JNIEnv * jni_env) {

	// resolve class id
	jint class_id = _get_class_id_for_object(obj, jni_env);

	// assign new net reference
	jlong net_ref =
			_set_net_reference(obj, jni_env, avail_object_id, class_id, 0);

	// increment object id counter
	++avail_object_id;

	return net_ref;
}

// can be used for any object - even classes
// NOTE
// This is not entirely true for class loaders.
// If the class loader net reference is used for class information resolving
// get_class_loader_net_ref should be used.
// However, this should not be necessary for objects received from java space
// using REDispatch.
static jlong get_net_reference(jobject obj, JNIEnv * jni_env) {

	if(obj == NULL) { // net reference for NULL is 0
		return 0;
	}

	jlong net_ref;

	enter_critical_section(jvmti_env, tagging_lock);
	{

		// get net reference
		net_ref = _get_net_reference(obj);

		// set net reference if necessary
		if(net_ref == 0) {

			// decide setting method
			if(object_is_class(obj, jni_env)) {
				// we have class object
				net_ref = _set_net_reference_for_class(obj, jni_env);
			}
			else {
				// we have non-class object
				net_ref = _set_net_reference_for_object(obj, jni_env);
			}
		}

	}
	exit_critical_section(jvmti_env, tagging_lock);

	return net_ref;
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

static void pack_object(buffer * buff, jobject to_send, JNIEnv * jni_env) {

	pack_long(buff, get_net_reference(to_send, jni_env));
}

static void pack_class(buffer * buff, jclass to_send, JNIEnv * jni_env) {

	jlong net_ref = get_net_reference(to_send, jni_env);

	pack_int(buff, net_ref_get_class_id(net_ref));
}

// ******************* analysis helper methods *******************

static jint analysis_start(jint analysis_method_id, JNIEnv * jni_env) {

	if(thread_id == 0) {

		// TODO tag thread + set highest bit
	}

	// get session id - free buffer pos
	jint sid = acquire_buff();

	buffer * buff = &buffs[sid];

	// analysis msg
	pack_int(buff, MSG_ANALYZE);

	// method id
	pack_int(buff, analysis_method_id);

	// TODO
	// thread id
	//pack_long(buff, thread_id);

	return sid;
}

static void analysis_end(jint sid) {

	send_buffer_schedule(&buffs[sid]);

	release_buff(sid);
}

// ******************* CLASS LOAD callback *******************

void JNICALL jvmti_callback_class_file_load_hook(jvmtiEnv *jvmti_env,
		JNIEnv* jni_env, jclass class_being_redefined, jobject loader,
		const char* name, jobject protection_domain, jint class_data_len,
		const unsigned char* class_data, jint* new_class_data_len,
		unsigned char** new_class_data) {

	// TODO instrument analysis classes

	// *** send class info ***

	// send new class message
	jint buff_id = acquire_buff();

	buffer * buff = &buffs[buff_id];

	// retrieve class loader net ref
	jlong loader_id = get_class_loader_net_ref(loader, jni_env);

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

	// send message
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

	error = (*jvmti_env)->SetEventCallbacks(jvmti_env, &callbacks, (jint) sizeof(callbacks));
	check_jvmti_error(jvmti_env, error, "Cannot set callbacks");

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

	error = (*jvmti_env)->CreateRawMonitor(jvmti_env, "object tags", &tagging_lock);
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

	return analysis_start(analysis_method_id, jni_env);
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

	pack_object(&buffs[sid], to_send, jni_env);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendClass
  (JNIEnv * jni_env, jclass this_class, jint sid, jclass to_send) {

	pack_class(&buffs[sid], to_send, jni_env);
}
