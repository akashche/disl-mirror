#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <netdb.h>
#include <unistd.h>
#include <netinet/tcp.h>

#include <pthread.h>

#include <jvmti.h>
#include <jni.h>

// has to be defined for jvmtihelper.h
#define ERR_PREFIX "DiSL-RE agent error: "

#include "../src-agent-c/jvmtihelper.h"
#include "../src-agent-c/comm.h"

// TODO ***** remove when trunk merged
#define TRUE 1
#define FALSE 0

#define ERR 10000

/*
 * Reports error if condition is true
 */
void check_error(int cond, const char *str) {

	if (cond) {

		fprintf(stderr, "%s%s\n", ERR_PREFIX, str);

		exit(ERR);
	}
}

// TODO *****

#include "messagetype.h"
#include "buffer.h"
#include "buffpack.h"
#include "blockingqueue.h"
#include "netref.h"

#include "dislreagent.h"

static const int ERR_SERVER = 10003;

// defaults - be sure that space in host_name is long enough
static const char * DEFAULT_HOST = "localhost";
static const char * DEFAULT_PORT = "11218";

// port and name of the instrumentation server
static char host_name[1024];
static char port_number[6]; // including final 0

// number of analysis requests in one message
static jint ANALYSIS_COUNT = 1024;

static jvmtiEnv * jvmti_env;
static JavaVM * java_vm;

static int jvm_started = FALSE;
static volatile int jvm_terminated = FALSE;

static volatile int no_tagging_work = FALSE;
static volatile int no_sending_work = FALSE;

// *** Accessed only by sending thread ***

// communication connection socket descriptor
// access must be protected by monitor
static int connection = 0;

// *** Sync queues ***

// !!! There should be enough buffers for initial class loading
// Sending thread is not jet running but buffers are consumed
#define BQ_BUFFERS 512

// queues contain process_buffs structure

// queues with empty buffers
static blocking_queue empty_buff_q;

// queue where buffers are queued for sending
static blocking_queue send_q;

// queue where buffers are queued for object
static blocking_queue objtag_q;

typedef struct {
	buffer * command_buff;
	buffer * analysis_buff;
	jlong thread_net_ref;
} process_buffs;

// list of all allocated bq buffers
static process_buffs pb_list[BQ_BUFFERS];

#define OT_OBJECT 1
#define OT_STRING 2
#define OT_CLASS 3

typedef struct {
	unsigned char obj_type;
	size_t buff_pos;
	jobject obj_to_tag;
} objtag_rec;

// *** Protected by tagging lock ***
// can require other locks while holding this

static jrawMonitorID tagging_lock;

// first available id for object tagging
static volatile jlong avail_object_id = 1;
static volatile jint avail_class_id = 1;

// first available id for new messages
static volatile jshort avail_analysis_id = 1;

// *** Thread locals ***

static __thread jlong t_net_ref = 0;
static __thread process_buffs * t_pb = NULL;
static __thread buffer * t_analysis_buff = NULL;
static __thread buffer * t_command_buff = NULL;
static __thread jint t_analysis_count = 0;
static __thread size_t t_analysis_count_pos = 0;

// *** Threads ***

static pthread_t objtag_thread;
static pthread_t send_thread;

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
		check_error(! fitsP, "Port number is too long");

		strcpy(port_number, port_start);
	}

	// check if host_name is big enough
	int fitsH = strlen(options) < sizeof(host_name);
	check_error(! fitsH, "Host name is too long");

	strcpy(host_name, options);
}

// ******************* Advanced buffer routines *******************

// thread_net_ref can have several states
// >  0 - means that buffer is owned by some thread that is tagged
// == 0 - means that buffer is owned by some thread that is NOT tagged

// == PB_FREE - means that buffer is currently free
static const jlong PB_FREE = -1;

// == PB_OBJTAG - means that buffer is scheduled (processed) for object tagging
static const jlong PB_OBJTAG = -100;

// == PB_SEND - means that buffer is scheduled (processed) for sending
static const jlong PB_SEND = -101;

static process_buffs * buffs_get() {

	process_buffs * buffs;

	// retrieves pointer to buffer
	bq_pop(&empty_buff_q, &buffs);

	buffs->thread_net_ref = t_net_ref;

	return buffs;
}

// only objtag or sending thread should access this function
static void _buffs_release(process_buffs * buffs) {

	// empty buff
	buffer_clean(buffs->analysis_buff);
	buffer_clean(buffs->command_buff);

	buffs->thread_net_ref = PB_FREE;

	// stores pointer to buffer
	bq_push(&empty_buff_q, &buffs);
}

static void buffs_objtag(process_buffs * buffs) {

	buffs->thread_net_ref = PB_OBJTAG;

	bq_push(&objtag_q, &buffs);
}

// only objtag thread should access this function
static process_buffs * _buffs_objtag_get() {

	process_buffs * buffs;

	bq_pop(&objtag_q, &buffs);

	return buffs;
}

static void buffs_send(process_buffs * buffs) {

	buffs->thread_net_ref = PB_SEND;

	bq_push(&send_q, &buffs);
}

// only sending thread should access this function
static process_buffs * _buffs_send_get() {

	process_buffs * buffs;

	bq_pop(&send_q, &buffs);

	return buffs;
}

// ******************* Advanced packing routines *******************

static void _fill_ot_rec(JNIEnv * jni_env, buffer * cmd_buff,
		unsigned char ot_type, buffer * buff, jstring to_send) {

	// crate object tagging record
	objtag_rec ot_rec;
	// type of object to be tagged
	ot_rec.obj_type = ot_type;
	// position in the buffer, where the data will be stored during tagging
	ot_rec.buff_pos = buffer_filled(buff);
	// global reference to the object to be tagged
	ot_rec.obj_to_tag = (*jni_env)->NewGlobalRef(jni_env, to_send);

	// save to command buff
	buffer_fill(cmd_buff, &ot_rec, sizeof(ot_rec));
}

static void pack_object(JNIEnv * jni_env, buffer * buff, buffer * cmd_buff,
		jobject to_send) {

	_fill_ot_rec(jni_env, cmd_buff, OT_OBJECT, buff, to_send);

	// pack dummy net reference
	pack_long(buff, 0);
}

static void pack_string_java(JNIEnv * jni_env, buffer * buff, buffer * cmd_buff,
		jstring to_send) {

	_fill_ot_rec(jni_env, cmd_buff, OT_STRING, buff, to_send);

	// pack dummy net reference
	pack_long(buff, 0);
}

static void pack_class(JNIEnv * jni_env, buffer * buff, buffer * cmd_buff,
		jclass to_send) {

	_fill_ot_rec(jni_env, cmd_buff, OT_CLASS, buff, to_send);

	// pack dummy class id
	pack_int(buff, 0);
}

static void buff_put_int(buffer * buff, size_t buff_pos, jint to_put) {

	jint nts = htonl(to_put);

	// put the long on the position in a proper format
	buffer_fill_at_pos(buff, buff_pos, &nts, sizeof(jint));
}

static void buff_put_long(buffer * buff, size_t buff_pos, jlong to_put) {

	// put the long on the position in a proper format
	jlong nts = htobe64(to_put);
	buffer_fill_at_pos(buff, buff_pos, &nts, sizeof(jlong));
}

// ******************* analysis helper methods *******************

static jshort register_method(JNIEnv * jni_env, jstring analysis_method_desc) {

	// *** send register analysis ***

	jshort new_analysis_id = 0;

	// get id for this method string
	// this could use different lock then tagging but it should not be a problem
	// and it will be used rarely - bit unoptimized
	enter_critical_section(jvmti_env, tagging_lock);
	{
		new_analysis_id = avail_analysis_id;
		++avail_analysis_id;
	}
	exit_critical_section(jvmti_env, tagging_lock);

	// send register analysis message

	// obtain buffer
	process_buffs * buffs = buffs_get();
	buffer * buff = buffs->analysis_buff;

	// msg id
	pack_byte(buff, MSG_REG_ANALYSIS);
	// new id for analysis method
	pack_short(buff, new_analysis_id);
	// method descriptor
	// uses string case and additional message for string - bit unoptimized
	pack_string_java(jni_env, buff, buffs->command_buff, analysis_method_desc);

	// send message
	buffs_objtag(buffs);

	return new_analysis_id;

}

static jlong tag_thread(JNIEnv * jni_env) {

	// obtain thread object
	jthread thread_obj;
	jvmtiError error = (*jvmti_env)->GetCurrentThread(jvmti_env, &thread_obj);
	check_jvmti_error(jvmti_env, error, "Cannot get object of current thread.");

	jlong thread_net_ref = 0;

	// obtain buffer - before tagging lock
	process_buffs * buffs = buffs_get();

	// tag the thread - with lock
	enter_critical_section(jvmti_env, tagging_lock);
	{
		// retrieve thread net reference
		thread_net_ref = get_net_reference(jni_env, jvmti_env,
				buffs->command_buff, thread_obj);

		// indicate that this thread has buffer allocated
		//  - useful for thread end hook
		net_ref_set_spec(&thread_net_ref, TRUE);
		update_net_reference(jvmti_env, thread_obj, thread_net_ref);

		// send new object tags
		buffs_send(buffs);
	}
	exit_critical_section(jvmti_env, tagging_lock);

	// free local reference
	(*jni_env)->DeleteLocalRef(jni_env, thread_obj);

	return thread_net_ref;
}

static void analysis_start(JNIEnv * jni_env, jshort analysis_method_id) {

	if(t_analysis_buff == NULL) {

		if(t_net_ref == 0) {

			t_net_ref = tag_thread(jni_env);
		}

		// get buffers
		t_pb = buffs_get();
		t_analysis_buff = t_pb->analysis_buff;
		t_command_buff = t_pb->command_buff;

		// Crate analysis message

		// analysis msg
		pack_byte(t_analysis_buff, MSG_ANALYZE);

		// thread id
		pack_long(t_analysis_buff, t_net_ref);

		// determines, how many analysis requests are send in one message
		t_analysis_count = 0;

		// get pointer to the location where count of requests will stored
		t_analysis_count_pos = buffer_filled(t_analysis_buff);

		// space initialization
		pack_int(t_analysis_buff, 0);
	}

	// analysis method desc
	pack_short(t_analysis_buff, analysis_method_id);
}

static void analysis_end() {

	// sending of half-full buffer is done in thread end hook

	// add number of completed requests
	++t_analysis_count;

	// buffer has to be updated each time because thread could end and buffer
	// has to be up-to date
	buff_put_int(t_analysis_buff, t_analysis_count_pos, t_analysis_count);

	// send only when the method count is reached
	if(t_analysis_count >= ANALYSIS_COUNT) {

		// invalidate buffer pointers
		t_analysis_buff = NULL;
		t_command_buff = NULL;

		// send buffers for object tagging
		buffs_objtag(t_pb);
	}
}

// ******************* Object tagging thread *******************

// TODO ! add cache - ??

static void ot_pack_string_cache(JNIEnv * jni_env, buffer * buff,
		jstring to_send, jlong str_net_ref) {

	// get string length
	jsize str_len = (*jni_env)->GetStringUTFLength(jni_env, to_send);

	// get string data as utf-8
	const char * str = (*jni_env)->GetStringUTFChars(jni_env, to_send, NULL);
	check_error(str == NULL, "Cannot get string from java");

	// check if the size is sendable
	int size_fits = str_len < UINT16_MAX;
	check_error(! size_fits, "Java string is too big for sending");

	// add message to the buffer

	// msg id
	pack_byte(buff, MSG_NEW_STRING);
	// send string net reference
	pack_long(buff, str_net_ref);
	// send string
	pack_string_utf8(buff, str, str_len);

	// release string
	(*jni_env)->ReleaseStringUTFChars(jni_env, to_send, str);
}

static void ot_tag_object(JNIEnv * jni_env, buffer * buff, size_t buff_pos,
		jobject to_send, buffer * new_objs_buff) {

	// get net reference and put it on proper position
	buff_put_long(buff, buff_pos,
			get_net_reference(jni_env, jvmti_env, new_objs_buff, to_send));
}

// NOTE: this tagging uses cache
static void ot_tag_string(JNIEnv * jni_env, buffer * buff, size_t buff_pos,
		jobject to_send, buffer * new_objs_buff) {

	jlong net_ref =
			get_net_reference(jni_env, jvmti_env, new_objs_buff, to_send);

	// test if the string was already sent to the server
	// NOTE: we don't use lock here, so it is possible that multiple threads
	//       will send it, but this will not hurt (only performance)
	if(net_ref_get_spec(net_ref) == FALSE) {

		// update the send status
		net_ref_set_spec(&net_ref, TRUE);
		update_net_reference(jvmti_env, to_send, net_ref);

		// add cached string to the buffer
		ot_pack_string_cache(jni_env, new_objs_buff, to_send, net_ref);
	}

	buff_put_long(buff, buff_pos, net_ref);
}

static void ot_tag_class(JNIEnv * jni_env, buffer * buff, size_t buff_pos,
		jobject to_send, buffer * new_objs_buff) {

	// get class net reference...
	jlong net_ref =
			get_net_reference(jni_env, jvmti_env, new_objs_buff, to_send);

	// ... and put it on proper position
	buff_put_int(buff, buff_pos, net_ref_get_class_id(net_ref));
}

static void ot_tag_buff(JNIEnv * jni_env, buffer * anl_buff, buffer * cmd_buff,
		buffer * new_objs_buff) {

	size_t cmd_buff_len = buffer_filled(cmd_buff);
	size_t read = 0;

	objtag_rec ot_rec;

	while(read < cmd_buff_len) {

		// read ot_rec data
		buffer_read(cmd_buff, read, &ot_rec, sizeof(ot_rec));
		read += sizeof(ot_rec);

		// tag
		switch(ot_rec.obj_type) {
		case OT_OBJECT: {
			ot_tag_object(jni_env, anl_buff, ot_rec.buff_pos, ot_rec.obj_to_tag,
					new_objs_buff);
			break;
		}
		case OT_STRING: {
			ot_tag_string(jni_env, anl_buff, ot_rec.buff_pos, ot_rec.obj_to_tag,
					new_objs_buff);
			break;
		}
		case OT_CLASS: {
			ot_tag_class(jni_env, anl_buff, ot_rec.buff_pos, ot_rec.obj_to_tag,
					new_objs_buff);
			break;
		}
		default:
			check_error(TRUE, "Undefined type to pack.");
		}

		// free global reference
		(*jni_env)->DeleteGlobalRef(jni_env, ot_rec.obj_to_tag);
	}
}

static void * objtag_thread_loop(void * obj) {

	// attach thread to jvm
	JNIEnv *jni_env;
	jvmtiError error = (*java_vm)->AttachCurrentThreadAsDaemon(java_vm,
			(void **)&jni_env, NULL);
	check_jvmti_error(jvmti_env, error, "Unable to attach objtag thread.");

	// one spare buffer for new objects
	buffer * new_obj_buff = malloc(sizeof(buffer));
	buffer_alloc(new_obj_buff);

	// exit when the jvm is terminated and there are no msg to process
	while(! (no_tagging_work && bq_length(&objtag_q) == 0) ) {

		// get buffer - before tagging lock
		process_buffs * pb = _buffs_objtag_get();

		// tag the objects - with lock
		enter_critical_section(jvmti_env, tagging_lock);
		{

			// tag objcects from buffer
			// note that analysis buffer is not required
			ot_tag_buff(jni_env, pb->analysis_buff, pb->command_buff,
					new_obj_buff);

			// exchange command_buff and new_obj_buff
			buffer * tmp = pb->command_buff;
			pb->command_buff = new_obj_buff;
			new_obj_buff = tmp;

			// clean new new_obj_buff
			buffer_clean(new_obj_buff);

			// send buffer
			buffs_send(pb);
		}
		exit_critical_section(jvmti_env, tagging_lock);
	}

	buffer_free(new_obj_buff);
	free(new_obj_buff);
	new_obj_buff = NULL;

	return NULL;
}

// ******************* Sending thread *******************

static void _send_buffer(buffer * b) {

	// send data
	send_data(connection, b->buff, b->occupied);
}

static int open_connection() {

	// get host address
	struct addrinfo * addr;
	int gai_res = getaddrinfo(host_name, port_number, NULL, &addr);
	check_error(gai_res != 0, gai_strerror(gai_res));

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

	// obtain buffer
	process_buffs * buffs = buffs_get();
	buffer * buff = buffs->command_buff;

	// msg id
	pack_byte(buff, MSG_CLOSE);

	// send buffer directly
	_send_buffer(buff);

	// release buffer
	_buffs_release(buffs);

	// close socket
	close(conn);
}

static void * send_thread_loop(void * obj) {

	connection = open_connection();

	// attach thread to jvm
	JNIEnv *jni_env;
	jvmtiError error = (*java_vm)->AttachCurrentThreadAsDaemon(java_vm,
			(void **)&jni_env, NULL);
	check_jvmti_error(jvmti_env, error, "Unable to attach send thread.");

	// exit when the jvm is terminated and there are no msg to process
	while(! (no_sending_work && bq_length(&send_q) == 0) ) {

		// get buffer
		process_buffs * pb = _buffs_send_get();

		// first send command buffer - contains new class or object ids,...
		_send_buffer(pb->command_buff);
		// send analysis buffer
		_send_buffer(pb->analysis_buff);

		// release buffer
		_buffs_release(pb);
	}

	// close connection
	close_connection(connection);

	return NULL;
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

	// obtain buffer - before tagging lock
	process_buffs * buffs = buffs_get();
	buffer * buff = buffs->analysis_buff;

	// tag the class loader - with lock
	enter_critical_section(jvmti_env, tagging_lock);
	{
		// retrieve class loader net ref
		jlong loader_id = 0;

		// this callback can be called before the jvm is started
		// the loaded classes are mostly java.lang.*
		// classes will be (hopefully) loaded by the same class loader
		// this phase is indicated by 0 in the class loader id and it is then
		// handled by server
		if(jvm_started) {
			loader_id = get_net_reference(jni_env, jvmti_env,
					buffs->command_buff, loader);
		}

		// msg id
		pack_byte(buff, MSG_NEW_CLASS);
		// class name
		pack_string_utf8(buff, name, strlen(name));
		// class loader id
		pack_long(buff, loader_id);
		// class code length
		pack_int(buff, class_data_len);
		// class code
		pack_bytes(buff, class_data, class_data_len);

		// send message
		buffs_send(buffs);
	}
	exit_critical_section(jvmti_env, tagging_lock);
}

// ******************* OBJECT FREE callback *******************

void JNICALL jvmti_callback_object_free_hook(jvmtiEnv *jvmti_env,
		jlong tag) {

	// send new obj free message

	// TODO ! buffer more msgs - ??
	// obtain buffer
	process_buffs * buffs = buffs_get();
	buffer * buff = buffs->command_buff;

	// msg id
	pack_byte(buff, MSG_OBJ_FREE);
	// obj id
	pack_long(buff, tag);

	// send message
	buffs_send(buffs);
}

// ******************* START callback *******************

void JNICALL jvmti_callback_vm_start_hook(jvmtiEnv *jvmti_env,
		JNIEnv* jni_env) {

	jvm_started = TRUE;
}

// ******************* INIT callback *******************

void JNICALL jvmti_callback_vm_init_hook(jvmtiEnv *jvmti_env,
		JNIEnv* jni_env, jthread thread) {

	// init object tagging thread
	int pc1 = pthread_create(&objtag_thread, NULL, objtag_thread_loop, NULL);
	check_error(pc1 != 0, "Cannot create tagging thread");

	// init sending thread
	int pc2 = pthread_create(&send_thread, NULL, send_thread_loop, NULL);
	check_error(pc2 != 0, "Cannot create sending thread");
}

// ******************* SHUTDOWN callback *******************

void JNICALL jvmti_callback_vm_death_hook(jvmtiEnv *jvmti_env,
		JNIEnv* jni_env) {

	// TODO ! send obj free buff

	// shutdown - first tagging then sending thread

	jvm_terminated = TRUE;

	no_tagging_work = TRUE;

	// send empty buff to obj_tag thread -> ensures exit if waiting
	process_buffs * buffs = buffs_get();
	buffs_objtag(buffs);

	// cleanup threads
	int rc1 = pthread_join(objtag_thread, NULL);
	check_error(rc1 != 0, "Cannot join tagging thread.");

	no_sending_work = TRUE;

	// TODO if multiple sending threads, multiple empty buffers have to be send
	// TODO also the buffers should be numbered according to the arrival to the
	// sending queue - has to be supported by the queue itself

	// send empty buff to obj_tag thread -> ensures exit if waiting
	buffs = buffs_get();
	buffs_send(buffs);

	int rc2 = pthread_join(send_thread, NULL);
	check_error(rc2 != 0, "Cannot join sending thread.");

	// NOTE: Buffers hold by other threads can be in inconsistent state.
	// We cannot simply send them, so we at least inform the user.

	// inform about all non-send buffers
	// all buffers should be send except some daemon thread buffers
	//  - also some class loading + thread tagging buffers can be there (with 0)
	// Report: .

	int relevant_count = 0;
	int support_count = 0;

	int i; // C99 needed for in cycle definition :)
	for(i = 0; i < BQ_BUFFERS; ++i) {

		// buffer held by thread that performed (is doing) analysis
		//  - probabbly analysis data
		if(pb_list[i].thread_net_ref > 0) {
			relevant_count += buffer_filled(pb_list[i].analysis_buff);
			support_count += buffer_filled(pb_list[i].command_buff);
		}

		// buffer held by thread that did NOT perform analysis
		//  - support data
		if(pb_list[i].thread_net_ref == 0) {
			support_count += buffer_filled(pb_list[i].analysis_buff) +
				buffer_filled(pb_list[i].command_buff);
		}

		check_error(pb_list[i].thread_net_ref == PB_OBJTAG,
				"Unprocessed buffers left in object tagging queue");

		check_error(pb_list[i].thread_net_ref == PB_SEND,
				"Unprocessed buffers left in sending queue");
	}

	if(relevant_count > 0 || support_count > 0) {
		fprintf(stderr, "%s%s%d%s%d%s",
				"Warning: ",
				"Due to non-terminated daemon threads, ",
				relevant_count,
				" Bytes of relevant data and ",
				support_count,
				" Bytes of support data were lost.\n");
	}

	// NOTE: If we clean up, and daemon thread will use the structures,
	// it will crash. It is then better to leave it all as is.
	// dealloc buffers
	// cleanup blocking queues
	// cleanup java locks
}

// ******************* THREAD END callback *******************

void JNICALL jvmti_callback_thread_end_hook(jvmtiEnv *jvmti_env,
		JNIEnv* jni_env, jthread thread) {

	// send all non-send buffers associated with this thread

	jlong net_ref = get_tag(jvmti_env, thread);

	// valid net reference and thread worked with buffers
	if(net_ref != 0 && net_ref_get_spec(net_ref) == TRUE) {

		int i; // C99 needed for in cycle definition :)
		for(i = 0; i < BQ_BUFFERS; ++i) {

			// if buffer is owned by tagged thread, send it
			if(pb_list[i].thread_net_ref == net_ref) {
				buffs_objtag(&(pb_list[i]));
			}
		}
	}
}

// ******************* JVMTI entry method *******************

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {

	java_vm = jvm;
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
	callbacks.ObjectFree = &jvmti_callback_object_free_hook;
	callbacks.VMStart = &jvmti_callback_vm_start_hook;
	callbacks.VMInit = &jvmti_callback_vm_init_hook;
	callbacks.VMDeath = &jvmti_callback_vm_death_hook;
	callbacks.ThreadEnd = &jvmti_callback_thread_end_hook;

	error = (*jvmti_env)->SetEventCallbacks(jvmti_env, &callbacks,
			(jint) sizeof(callbacks));
	check_jvmti_error(jvmti_env, error, "Cannot set callbacks");

	error = (*jvmti_env)->SetEventNotificationMode(jvmti_env, JVMTI_ENABLE,
			JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL);
	check_jvmti_error(jvmti_env, error, "Cannot set class load hook");

	error = (*jvmti_env)->SetEventNotificationMode(jvmti_env, JVMTI_ENABLE,
			JVMTI_EVENT_OBJECT_FREE, NULL);
	check_jvmti_error(jvmti_env, error, "Cannot set object free hook");

	error = (*jvmti_env)->SetEventNotificationMode(jvmti_env, JVMTI_ENABLE,
			JVMTI_EVENT_VM_START, NULL);
	check_jvmti_error(jvmti_env, error, "Cannot set jvm start hook");

	error = (*jvmti_env)->SetEventNotificationMode(jvmti_env, JVMTI_ENABLE,
			JVMTI_EVENT_VM_INIT, NULL);
	check_jvmti_error(jvmti_env, error, "Cannot set jvm init hook");

	error = (*jvmti_env)->SetEventNotificationMode(jvmti_env, JVMTI_ENABLE,
			JVMTI_EVENT_VM_DEATH, NULL);
	check_jvmti_error(jvmti_env, error, "Cannot set jvm death hook");

	error = (*jvmti_env)->SetEventNotificationMode(jvmti_env, JVMTI_ENABLE,
			JVMTI_EVENT_THREAD_END, NULL);
	check_jvmti_error(jvmti_env, error, "Cannot set thread end hook");

	error = (*jvmti_env)->CreateRawMonitor(jvmti_env, "object tags",
			&tagging_lock);
	check_jvmti_error(jvmti_env, error, "Cannot create raw monitor");

	// read options (port/hostname)
	parse_agent_options(options);

	// init blocking queues
	bq_create(jvmti_env, &empty_buff_q, BQ_BUFFERS, sizeof(process_buffs *));
	bq_create(jvmti_env, &objtag_q, BQ_BUFFERS, sizeof(process_buffs *));
	bq_create(jvmti_env, &send_q, BQ_BUFFERS, sizeof(process_buffs *));

	// allocate buffers and add to the empty buffer queue
	int i; // C99 needed for in cycle definition :)
	for(i = 0; i < BQ_BUFFERS; ++i) {

		process_buffs * pb = &(pb_list[i]);

		// allocate space for buffer struct
		pb->analysis_buff = malloc(sizeof(buffer));
		// allocate buffer
		buffer_alloc(pb->analysis_buff);

		// allocate space for buffer struct
		pb->command_buff = malloc(sizeof(buffer));
		// allocate buffer
		buffer_alloc(pb->command_buff);

		// add buffer to the empty queue
		_buffs_release(pb);
	}

	return 0;
}

// ******************* REDispatch methods *******************

JNIEXPORT jshort JNICALL Java_ch_usi_dag_dislre_REDispatch_registerMethod
  (JNIEnv * jni_env, jclass this_class, jstring analysis_method_desc) {

	return register_method(jni_env, analysis_method_desc);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_analysisStart
  (JNIEnv * jni_env, jclass this_class, jshort analysis_method_id) {

	analysis_start(jni_env, analysis_method_id);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_analysisEnd
  (JNIEnv * jni_env, jclass this_class) {

	analysis_end();
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendBoolean
  (JNIEnv * jni_env, jclass this_class, jboolean to_send) {

	pack_boolean(t_analysis_buff, to_send);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendByte
  (JNIEnv * jni_env, jclass this_class, jbyte to_send) {

	pack_byte(t_analysis_buff, to_send);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendChar
  (JNIEnv * jni_env, jclass this_class, jchar to_send) {

	pack_char(t_analysis_buff, to_send);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendShort
  (JNIEnv * jni_env, jclass this_class, jshort to_send) {

	pack_short(t_analysis_buff, to_send);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendInt
  (JNIEnv * jni_env, jclass this_class, jint to_send) {

	pack_int(t_analysis_buff, to_send);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendLong
  (JNIEnv * jni_env, jclass this_class, jlong to_send) {

	pack_long(t_analysis_buff, to_send);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendFloatAsInt
  (JNIEnv * jni_env, jclass this_class, jint to_send) {

	pack_int(t_analysis_buff, to_send);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendDoubleAsLong
  (JNIEnv * jni_env, jclass this_class, jlong to_send) {

	pack_long(t_analysis_buff, to_send);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendString
  (JNIEnv * jni_env, jclass this_class, jstring to_send) {

	pack_string_java(jni_env, t_analysis_buff, t_command_buff, to_send);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendObject
  (JNIEnv * jni_env, jclass this_class, jobject to_send) {

	pack_object(jni_env, t_analysis_buff, t_command_buff, to_send);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendClass
  (JNIEnv * jni_env, jclass this_class, jclass to_send) {

	pack_class(jni_env, t_analysis_buff, t_command_buff, to_send);
}
