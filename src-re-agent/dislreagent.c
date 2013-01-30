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

static volatile int no_tagging_work = FALSE;
static volatile int no_sending_work = FALSE;

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
	jlong owner_id;
} process_buffs;

// list of all allocated bq buffers
static process_buffs pb_list[BQ_BUFFERS];

#define OT_OBJECT 1
#define OT_DATA_OBJECT 2

typedef struct {
	unsigned char obj_type;
	size_t buff_pos;
	jobject obj_to_tag;
} objtag_rec;

// *** buffers for total ordering ***

typedef struct {
	process_buffs * pb;
	jint analysis_count;
	size_t analysis_count_pos;
} to_buff_struct;

#define INVALID_BUFF_ID -1

#define TO_BUFFER_MAX_ID 127 // byte is the holding type
#define TO_BUFFER_COUNT (TO_BUFFER_MAX_ID + 1) // +1 for buffer id 0

static jrawMonitorID to_buff_lock;

static to_buff_struct to_buff_array[TO_BUFFER_COUNT];

// *** Protected by tagging lock ***
// can require other locks while holding this

#define NULL_NET_REF 0

static jrawMonitorID tagging_lock;

// first available id for object tagging
static volatile jlong avail_object_id = 1;
static volatile jint avail_class_id = 1;

// first available id for new messages
static volatile jshort avail_analysis_id = 1;

// *** Thread ids ***

#define INVALID_THREAD_ID -1

#define STARTING_THREAD_ID (TO_BUFFER_MAX_ID + 1)

// initial ids are reserved for total ordering buffers
static volatile jlong avail_thread_id = STARTING_THREAD_ID;


// *** Thread locals ***

// NOTE: The JVMTI functionality allows to implement everything
// using JVM, but the GNU implementation is faster and WORKING


struct tldata {
	jlong id;
	process_buffs * local_pb;
	jbyte to_buff_id;
	process_buffs * pb;
	buffer * analysis_buff;
	buffer * command_buff;
	jint analysis_count;
	size_t analysis_count_pos;
	size_t args_length_pos;
};


#if defined (__APPLE__) && defined (__MACH__)

//
// Use pthreads on Mac OS X
//

static pthread_key_t tls_key;


static void tls_init () {
	int result = pthread_key_create (& tls_key, NULL);
	check_error(result != 0, "Failed to allocate thread-local storage key");
}


inline static struct tldata * tld_init (struct tldata * tld) {
	tld->id= INVALID_THREAD_ID;
	tld->local_pb = NULL;
	tld->to_buff_id = INVALID_BUFF_ID;
	tld->pb = NULL;
	tld->analysis_buff = NULL;
	tld->analysis_count = 0;
	tld->analysis_count_pos = 0;

	return tld;
}

static struct tldata * tld_create ()  {
	struct tldata * tld = malloc (sizeof (struct tldata));
	check_error (tld == NULL, "Failed to allocate thread-local data");
	int result = pthread_setspecific (tls_key, tld);
	check_error (result != 0, "Failed to store thread-local data");
	return tld_init (tld);
}

inline static struct tldata * tld_get () {
	struct tldata * tld = pthread_getspecific (tls_key);
	return (tld != NULL) ? tld : tld_create ();
}

#else

//
// Use GNU __thread where supported
//

static void tls_init () {
	// empty
}


static __thread struct tldata tld = {
		.id = INVALID_THREAD_ID,
		.local_pb = NULL,
		.to_buff_id = INVALID_BUFF_ID,
		.pb = NULL,
		.analysis_buff = NULL,
		.command_buff = NULL,
		.analysis_count = 0,
		.analysis_count_pos = 0,
};

inline static struct tldata * tld_get () {
	return & tld;
}

#endif


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

// owner_id can have several states
// > 0 && <= TO_BUFFER_MAX_ID
//    - means that buffer is reserved for total ordering events
// >= STARTING_THREAD_ID
//    - means that buffer is owned by some thread that is marked
// == -1 - means that buffer is owned by some thread that is NOT tagged

// == PB_FREE - means that buffer is currently free
static const jlong PB_FREE = -2;

// == PB_OBJTAG - means that buffer is scheduled (processed) for object tagging
static const jlong PB_OBJTAG = -100;

// == PB_SEND - means that buffer is scheduled (processed) for sending
static const jlong PB_SEND = -101;

static process_buffs * buffs_get(jlong thread_id) {
#ifdef DEBUG
	printf("Acquiring buffer -- empty (thread %ld)\n", tld_get()->id);
#endif

	// retrieves pointer to buffer
	process_buffs * buffs;
	bq_pop(&empty_buff_q, &buffs);

	buffs->owner_id = thread_id;

#ifdef DEBUG
	printf("Buffer acquired -- empty (thread %ld)\n", tld_get()->id);
#endif

	return buffs;
}

// only objtag or sending thread should access this function
static void _buffs_release(process_buffs * buffs) {
#ifdef DEBUG
	printf("Queuing buffer -- empty (thread %ld)\n", tld_get()->id);
#endif

	// empty buff
	buffer_clean(buffs->analysis_buff);
	buffer_clean(buffs->command_buff);

	// stores pointer to buffer
	buffs->owner_id = PB_FREE;
	bq_push(&empty_buff_q, &buffs);

#ifdef DEBUG
	printf("Buffer queued -- empty (thread %ld)\n", tld_get()->id);
#endif
}

static void buffs_objtag(process_buffs * buffs) {
#ifdef DEBUG
	printf("Queuing buffer -- objtag (thread %ld)\n", tld_get()->id);
#endif

	buffs->owner_id = PB_OBJTAG;
	bq_push(&objtag_q, &buffs);

#ifdef DEBUG
	printf("Buffer queued -- objtag (thread %ld)\n", tld_get()->id);
#endif
}

// only objtag thread should access this function
static process_buffs * _buffs_objtag_get() {
#ifdef DEBUG
	printf("Acquiring buffer -- objtag (thread %ld)\n", tld_get()->id);
#endif

	process_buffs * buffs;
	bq_pop(&objtag_q, &buffs);

#ifdef DEBUG
	printf("Buffer acquired -- objtag (thread %ld)\n", tld_get()->id);
#endif

	return buffs;
}

static void buffs_send(process_buffs * buffs) {
#ifdef DEBUG
	printf("Queuing buffer -- send (thread %ld)\n", tld_get()->id);
#endif

	buffs->owner_id = PB_SEND;
	bq_push(&send_q, &buffs);

#ifdef DEBUG
	printf("Buffer queued -- send (thread %ld)\n", tld_get()->id);
#endif
}

// only sending thread should access this function
static process_buffs * _buffs_send_get() {
#ifdef DEBUG
	printf("Acquiring buffer -- send (thread %ld)\n", tld_get()->id);
#endif

	process_buffs * buffs;
	bq_pop(&send_q, &buffs);

#ifdef DEBUG
	printf("Buffer acquired -- send (thread %ld)\n", tld_get()->id);
#endif

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
		jobject to_send, unsigned char object_type) {

	// create entry for object tagging thread that will replace the null ref
	if(to_send != NULL) {
		_fill_ot_rec(jni_env, cmd_buff, object_type, buff, to_send);
	}

	// pack null net reference
	pack_long(buff, NULL_NET_REF);
}

static void buff_put_short(buffer * buff, size_t buff_pos, jshort to_put) {
	// put the short at the position in network order
	jshort nts = htons(to_put);
	buffer_fill_at_pos(buff, buff_pos, &nts, sizeof(jshort));
}


static void buff_put_int(buffer * buff, size_t buff_pos, jint to_put) {
	// put the int at the position in network order
	jint nts = htonl(to_put);
	buffer_fill_at_pos(buff, buff_pos, &nts, sizeof(jint));
}

static void buff_put_long(buffer * buff, size_t buff_pos, jlong to_put) {
	// put the long at the position in network order
	jlong nts = htobe64(to_put);
	buffer_fill_at_pos(buff, buff_pos, &nts, sizeof(jlong));
}


// ******************* analysis helper methods *******************

static jshort next_analysis_id () {
	// get id for this method string
	// this could use different lock then tagging but it should not be a problem
	// and it will be used rarely - bit unoptimized

	jshort result = -1;
	enter_critical_section(jvmti_env, tagging_lock);
	{
		result = avail_analysis_id++;
	}
	exit_critical_section(jvmti_env, tagging_lock);

	return result;
}

static jshort register_method(
		JNIEnv * jni_env, jstring analysis_method_desc,
		jlong thread_id
) {
#ifdef DEBUG
	printf("Registering method (thread %ld)\n", tld_get()->id);
#endif

	// *** send register analysis ***

	jshort new_analysis_id = next_analysis_id ();

	// send register analysis message

	// obtain buffer
	process_buffs * buffs = buffs_get(thread_id);
	buffer * buff = buffs->analysis_buff;

	// msg id
	pack_byte(buff, MSG_REG_ANALYSIS);
	// new id for analysis method
	pack_short(buff, new_analysis_id);
	// method descriptor
	// sends string as object with additional data - bit unoptimized but works
	pack_object(jni_env, buff, buffs->command_buff, analysis_method_desc,
			OT_DATA_OBJECT);

	// send message
	buffs_objtag(buffs);

#ifdef DEBUG
	printf("Method registered (thread %ld)\n", tld_get()->id);
#endif

	return new_analysis_id;
}


static jlong next_thread_id () {
#ifdef DEBUG
	printf("Marking thread (thread %ld)\n", tld_get()->id);
#endif
	// mark the thread - with lock
	// TODO replace total ordering lock with private lock - perf. issue
	jlong result = -1;
	enter_critical_section(jvmti_env, to_buff_lock);
	{
		result = avail_thread_id++;
	}
	exit_critical_section(jvmti_env, to_buff_lock);

#ifdef DEBUG
	printf("Thread marked (thread %ld)\n", result);
#endif
	return result;
}


static size_t createAnalysisRequestHeader (
		buffer * buff, jshort analysis_method_id
) {
	// analysis method id
	pack_short(buff, analysis_method_id);

	// position of the short indicating the length of marshalled arguments
	size_t pos = buffer_filled(buff);

	// initial value of the length of the marshalled arguments
	pack_short(buff, 0xBAAD);

	return pos;
}


void analysis_start_buff(
		JNIEnv * jni_env, jshort analysis_method_id, jbyte ordering_id,
		struct tldata * tld
) {
#ifdef DEBUG
	printf("Analysis (buffer) start enter (thread %ld)\n", tld_get()->id);
#endif

	check_error(ordering_id < 0, "Buffer id has negative value");

	// flush normal buffers before each global buffering
	if(tld->analysis_buff != NULL) {
		// invalidate buffer pointers
		tld->analysis_buff = NULL;
		tld->command_buff = NULL;

		// send buffers for object tagging
		buffs_objtag(tld->pb);

		// invalidate buffer pointer
		tld->pb = NULL;
	}

	// allocate special local buffer for this buffering
	if(tld->local_pb == NULL) {
		// mark thread
		if(tld->id == INVALID_THREAD_ID) {
			tld->id = next_thread_id ();
		}

		// get buffers
		tld->local_pb = buffs_get(tld->id);
	}

	// set local buffers for this buffering
	tld->analysis_buff = tld->local_pb->analysis_buff;
	tld->command_buff = tld->local_pb->command_buff;

	tld->to_buff_id = ordering_id;

	// create request header, keep track of the position
	// of the length of marshalled arguments
	tld->args_length_pos = createAnalysisRequestHeader(tld->analysis_buff, analysis_method_id);

#ifdef DEBUG
	printf("Analysis (buffer) start exit (thread %ld)\n", tld_get()->id);
#endif
}


static size_t createAnalysisMsg(buffer * buff, jlong id) {
	// create analysis message

	// analysis msg
	pack_byte(buff, MSG_ANALYZE);

	// thread (total order buffer) id
	pack_long(buff, id);

	// get pointer to the location where count of requests will stored
	size_t pos = buffer_filled(buff);

	// request count space initialization
	pack_int(buff, 0xBAADF00D);

	return pos;
}



static void analysis_start(
		JNIEnv * jni_env, jshort analysis_method_id,
		struct tldata * tld
) {
#ifdef DEBUG
	printf("Analysis start enter (thread %ld)\n", tld_get()->id);
#endif

	if(tld->analysis_buff == NULL) {

		// mark thread
		if(tld->id == INVALID_THREAD_ID) {
			tld->id = next_thread_id ();
		}

		// get buffers
		tld->pb = buffs_get(tld->id);
		tld->analysis_buff = tld->pb->analysis_buff;
		tld->command_buff = tld->pb->command_buff;

		// determines, how many analysis requests are sent in one message
		tld->analysis_count = 0;

		// create analysis message
		tld->analysis_count_pos = createAnalysisMsg(tld->analysis_buff, tld->id);
	}

	// create request header, keep track of the position
	// of the length of marshalled arguments
	tld->args_length_pos = createAnalysisRequestHeader(tld->analysis_buff, analysis_method_id);

#ifdef DEBUG
	printf("Analysis start exit (thread %ld)\n", tld_get()->id);
#endif
}

static void correct_cmd_buff_pos(buffer * cmd_buff, size_t shift) {

	size_t cmd_buff_len = buffer_filled(cmd_buff);
	size_t read = 0;

	objtag_rec ot_rec;

	// go through all records and shift the buffer position
	while(read < cmd_buff_len) {

		// read ot_rec data
		buffer_read(cmd_buff, read, &ot_rec, sizeof(ot_rec));

		// shift buffer position
		ot_rec.buff_pos += shift;

		// write ot_rec data
		buffer_fill_at_pos(cmd_buff, read, &ot_rec, sizeof(ot_rec));

		// next
		read += sizeof(ot_rec);
	}
}

static void analysis_end_buff(struct tldata * tld) {
#ifdef DEBUG
	printf("Analysis (buffer) end enter (thread %ld)\n", tld_get()->id);
#endif

	// TODO lock for each buffer id

	// sending of half-full buffer is done in shutdown hook and obj free hook

	// write analysis to total order buffer - with lock
	enter_critical_section(jvmti_env, to_buff_lock);
	{
		// pointer to the total order buffer structure
		to_buff_struct * tobs = &(to_buff_array[tld->to_buff_id]);

		// allocate new buffer
		if(tobs->pb == NULL) {

			tobs->pb = buffs_get(tld->id);

			// set owner_id as t_buffid
			tobs->pb->owner_id = tld->to_buff_id;

			// determines, how many analysis requests are sent in one message
			tobs->analysis_count = 0;

			// create analysis message
			tobs->analysis_count_pos = createAnalysisMsg(
					tobs->pb->analysis_buff, tld->to_buff_id);
		}

		// first correct positions in command buffer
		// records in command buffer are positioned according to the local
		// analysis buffer but we want the position to be valid in total ordered
		// buffer
		correct_cmd_buff_pos(tld->local_pb->command_buff,
				buffer_filled(tobs->pb->analysis_buff));

		// fill total order buffers
		buffer_fill(tobs->pb->analysis_buff,
				// NOTE: normally access the buffer using methods
				tld->local_pb->analysis_buff->buff,
				tld->local_pb->analysis_buff->occupied);

		buffer_fill(tobs->pb->command_buff,
				// NOTE: normally access the buffer using methods
				tld->local_pb->command_buff->buff,
				tld->local_pb->command_buff->occupied);

		// empty local buffers
		buffer_clean(tld->local_pb->analysis_buff);
		buffer_clean(tld->local_pb->command_buff);

		// add number of completed requests
		++(tobs->analysis_count);

		// buffer has to be updated each time because jvm could end and buffer
		// has to be up-to date
		buff_put_int(tobs->pb->analysis_buff, tobs->analysis_count_pos,
				tobs->analysis_count);

		// send only when the method count is reached
		if(tobs->analysis_count >= ANALYSIS_COUNT) {

			// send buffers for object tagging
			buffs_objtag(tobs->pb);

			// invalidate buffer pointer
			tobs->pb = NULL;
		}
	}
	exit_critical_section(jvmti_env, to_buff_lock);

	// reset analysis and command buffers for normal buffering
	// set to NULL, because we've send the buffers at the beginning of
	// global buffer buffering
	tld->analysis_buff = NULL;
	tld->command_buff = NULL;

	// invalidate buffer id
	tld->to_buff_id = INVALID_BUFF_ID;

#ifdef DEBUG
	printf("Analysis (buffer) end exit (thread %ld)\n", tld_get()->id);
#endif
}

static void analysis_end(struct tldata * tld) {
	// update the length of the marshalled arguments
	jshort args_length = buffer_filled(tld->analysis_buff) - tld->args_length_pos - sizeof (jshort);
	buff_put_short(tld->analysis_buff, tld->args_length_pos, args_length);

	// this method is also called for end of analysis for totally ordered API
	if(tld->to_buff_id != INVALID_BUFF_ID) {
		analysis_end_buff(tld);
		return;
	}

#ifdef DEBUG
	printf("Analysis end enter (thread %ld)\n", tld_get()->id);
#endif

	// sending of half-full buffer is done in thread end hook

	// increment the number of completed requests
	tld->analysis_count++;

	// buffer has to be updated each time - the thread can end any time
	buff_put_int(tld->analysis_buff, tld->analysis_count_pos, tld->analysis_count);

	// send only after the proper count is reached
	if(tld->analysis_count >= ANALYSIS_COUNT) {
		// invalidate buffer pointers
		tld->analysis_buff = NULL;
		tld->command_buff = NULL;

		// send buffers for object tagging
		buffs_objtag(tld->pb);

		// invalidate buffer pointer
		tld->pb = NULL;
	}

#ifdef DEBUG
	printf("Analysis end exit (thread %ld)\n", tld_get()->id);
#endif
}

// ******************* Object tagging thread *******************

// TODO add cache - ??

static jclass THREAD_CLASS = NULL;
static jclass STRING_CLASS = NULL;

static void ot_pack_string_data(JNIEnv * jni_env, buffer * buff,
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

static void ot_pack_thread_data(JNIEnv * jni_env, buffer * buff,
		jstring to_send, jlong thr_net_ref) {

	jvmtiThreadInfo info;
	jvmtiError error = (*jvmti_env)->GetThreadInfo(jvmti_env, to_send, &info);
	check_error(error != JVMTI_ERROR_NONE, "Cannot get tread info");

	// pack thread info message

	// msg id
	pack_byte(buff, MSG_THREAD_INFO);

	// thread object id
	pack_long(buff, thr_net_ref);

	// thread name
	pack_string_utf8(buff, info.name, strlen(info.name));

	// is daemon thread
	pack_boolean(buff, info.is_daemon);
}

static void update_send_status(jobject to_send, jlong * net_ref) {

	net_ref_set_spec(net_ref, TRUE);
	update_net_reference(jvmti_env, to_send, *net_ref);
}

static void ot_pack_aditional_data(JNIEnv * jni_env, jlong * net_ref,
		jobject to_send, unsigned char obj_type, buffer * new_objs_buff) {

	// NOTE: we don't use lock for updating send status, so it is possible
	// that multiple threads will send it, but this will hurt only performance

	// test if the data was already sent to the server
	if(net_ref_get_spec(*net_ref) == TRUE) {
		return;
	}

	// NOTE: Tests for class types could be done by buffering threads.
	//       It depends, where we want to have the load.

	// String - pack data
	if((*jni_env)->IsInstanceOf(jni_env, to_send, STRING_CLASS)) {

		update_send_status(to_send, net_ref);
		ot_pack_string_data(jni_env, new_objs_buff, to_send, *net_ref);
	}

	// Thread - pack data
	if((*jni_env)->IsInstanceOf(jni_env, to_send, THREAD_CLASS)) {

		update_send_status(to_send, net_ref);
		ot_pack_thread_data(jni_env, new_objs_buff, to_send, *net_ref);
	}
}

static void ot_tag_record(JNIEnv * jni_env, buffer * buff, size_t buff_pos,
		jobject to_send, unsigned char obj_type, buffer * new_objs_buff) {

	// get net reference
	jlong net_ref =
			get_net_reference(jni_env, jvmti_env, new_objs_buff, to_send);

	// send additional data
	if(obj_type == OT_DATA_OBJECT) {

		// NOTE: can update net reference (net_ref)
		ot_pack_aditional_data(jni_env, &net_ref, to_send, obj_type,
				new_objs_buff);
	}

	// update the net reference
	buff_put_long(buff, buff_pos, net_ref);
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

		ot_tag_record(jni_env, anl_buff, ot_rec.buff_pos, ot_rec.obj_to_tag,
				ot_rec.obj_type, new_objs_buff);

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

	// retrieve java types

	STRING_CLASS = (*jni_env)->FindClass(jni_env, "java/lang/String");
	check_error(STRING_CLASS == NULL, "String class not found");

	THREAD_CLASS = (*jni_env)->FindClass(jni_env, "java/lang/Thread");
	check_error(STRING_CLASS == NULL, "Thread class not found");

	// exit when the jvm is terminated and there are no msg to process
	while(! (no_tagging_work && bq_length(&objtag_q) == 0) ) {

		// get buffer - before tagging lock
		process_buffs * pb = _buffs_objtag_get();

#ifdef DEBUG
		printf("Object tagging started (thread %ld)\n", tld_get()->id);
#endif

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

#ifdef DEBUG
		printf("Object tagging ended (thread %ld)\n", tld_get()->id);
#endif
	}

	buffer_free(new_obj_buff);
	free(new_obj_buff);
	new_obj_buff = NULL;

	return NULL;
}

// ******************* Sending thread *******************

static void _send_buffer(int connection, buffer * b) {

	// send data
	// NOTE: normally access the buffer using methods
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

static void close_connection(int conn, jlong thread_id) {

	// send close message

	// obtain buffer
	process_buffs * buffs = buffs_get(thread_id);
	buffer * buff = buffs->command_buff;

	// msg id
	pack_byte(buff, MSG_CLOSE);

	// send buffer directly
	_send_buffer(conn, buff);

	// release buffer
	_buffs_release(buffs);

	// close socket
	close(conn);
}


static void attach_current_thread_to_jvm () {
	JNIEnv *jni_env;
	jvmtiError error = (*java_vm)->AttachCurrentThreadAsDaemon(
			java_vm, (void **)&jni_env, NULL
	);
	check_jvmti_error(jvmti_env, error, "Unable to attach send thread.");
}

static void * send_thread_loop(void * obj) {
	int connection = open_connection();
	attach_current_thread_to_jvm ();

	// exit when the jvm is terminated and there are no msg to process
	while(! (no_sending_work && bq_length(&send_q) == 0) ) {

		// get buffer
		// TODO thread could timeout here with timeout about 5 sec and check
		// if all of the buffers are allocated by the application threads
		// and all application threads are waiting on free buffer - deadlock
		process_buffs * pb = _buffs_send_get();

#ifdef DEBUG
		printf("Sending buffer (thread %ld)\n", tld_get()->id);
#endif

		// first send command buffer - contains new class or object ids,...
		_send_buffer(connection, pb->command_buff);
		// send analysis buffer
		_send_buffer(connection, pb->analysis_buff);

		// release buffer
		_buffs_release(pb);

#ifdef DEBUG
		printf("Buffer sent (thread %ld)\n", tld_get()->id);
#endif
	}

	// close connection
	close_connection(connection, tld_get()->id);
	return NULL;
}

// ******************* CLASS LOAD callback *******************

void JNICALL jvmti_callback_class_file_load_hook(
		jvmtiEnv *jvmti_env, JNIEnv* jni_env,
		jclass class_being_redefined, jobject loader,
		const char* name, jobject protection_domain,
		jint class_data_len, const unsigned char* class_data,
		jint* new_class_data_len, unsigned char** new_class_data
) {
	struct tldata * tld = tld_get();

	// TODO instrument analysis classes

#ifdef DEBUG
	printf("Sending new class (thread %ld)\n", tld_get()->id);
#endif

	// *** send class info ***

	// send new class message

	// obtain buffer - before tagging lock
	process_buffs * buffs = buffs_get(tld->id);
	buffer * buff = buffs->analysis_buff;

	// tag the class loader - with lock
	enter_critical_section(jvmti_env, tagging_lock);
	{
		// retrieve class loader net ref
		jlong loader_id = NULL_NET_REF;

		// this callback can be called before the jvm is started
		// the loaded classes are mostly java.lang.*
		// classes will be (hopefully) loaded by the same class loader
		// this phase is indicated by NULL_NET_REF in the class loader id and it
		// is then handled by server
		if(jvm_started) {
			loader_id = get_net_reference(
					jni_env, jvmti_env,
					buffs->command_buff, loader
			);
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

#ifdef DEBUG
	printf("New class sent (thread %ld)\n", tld_get()->id);
#endif
}


// ******************* OBJECT FREE callback *******************

void JNICALL jvmti_callback_object_free_hook(
		jvmtiEnv *jvmti_env, jlong tag
) {
#ifdef DEBUG
	printf("Sending object free (thread %ld)\n", tld_get()->id);
#endif

	// NOTE: we don't need to send any other buffer with this one because
	// in the buffers are only life objects (global references)

	// send new obj free message

	// TODO buffer more msgs (send buffer at shutdown) - ??
	// obtain buffer
	process_buffs * buffs = buffs_get(tld_get()->id);
	buffer * buff = buffs->analysis_buff;

	// msg id
	pack_byte(buff, MSG_OBJ_FREE);
	// obj id
	pack_long(buff, tag);

	// send message
	// NOTE !: It is critical for proper ordering to send the buffer to the
	// object tagging queue.
	// Explanation: It is guaranteed, that no buffer held by an analysis thread
	// has this object, because all buffers have references to the objects they
	// are holding. The object tagging thread is the one who is releasing the
	// references. It is then necessary, that this event is put to the sending
	// queue after the object tagging thread puts the currently processed buffer
	// to the sending queue. This is easily arranged by putting this buffer to
	// the object tagging queue.
	buffs_objtag(buffs);

#ifdef DEBUG
	printf("Object free sent (thread %ld)\n", tld_get()->id);
#endif
}


// ******************* START callback *******************

void JNICALL jvmti_callback_vm_start_hook(
		jvmtiEnv *jvmti_env, JNIEnv* jni_env
) {
	jvm_started = TRUE;
}


// ******************* INIT callback *******************

void JNICALL jvmti_callback_vm_init_hook(
		jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread
) {
#ifdef DEBUG
	printf("Starting worker threads (thread %ld)\n", tld_get()->id);
#endif

	// init object tagging thread
	int pc1 = pthread_create(&objtag_thread, NULL, objtag_thread_loop, NULL);
	check_error(pc1 != 0, "Cannot create tagging thread");

	// init sending thread
	int pc2 = pthread_create(&send_thread, NULL, send_thread_loop, NULL);
	check_error(pc2 != 0, "Cannot create sending thread");

#ifdef DEBUG
	printf("Worker threads started (thread %ld)\n", tld_get()->id);
#endif
}


// ******************* SHUTDOWN callback *******************

static void send_all_to_buffers() {

	// send all total ordering buffers - with lock
	enter_critical_section(jvmti_env, to_buff_lock);
	{
		int i;
		for(i = 0; i < TO_BUFFER_COUNT; ++i) {

			// send all buffers for occupied ids
			if(to_buff_array[i].pb != NULL) {

				// send buffers for object tagging
				buffs_objtag(to_buff_array[i].pb);

				// invalidate buffer pointer
				to_buff_array[i].pb = NULL;
			}
		}
	}
	exit_critical_section(jvmti_env, to_buff_lock);
}

static void send_thread_buffers(struct tldata * tld) {

	// thread is marked -> worked with buffers
	jlong thread_id = tld->id;
	if(thread_id != INVALID_THREAD_ID) {

		int i;
		for(i = 0; i < BQ_BUFFERS; ++i) {
			// if buffer is owned by tagged thread, send it
			if(pb_list[i].owner_id == thread_id) {
				buffs_objtag(&(pb_list[i]));
			}
		}
	}

	tld->analysis_buff = NULL;
	tld->command_buff = NULL;
	tld->pb = NULL;
}

void JNICALL jvmti_callback_vm_death_hook(
		jvmtiEnv *jvmti_env, JNIEnv* jni_env
) {
	struct tldata * tld = tld_get();

#ifdef DEBUG
	printf("Shutting down (thread %ld)\n", tld_get()->id);
#endif

	// send all buffers for total order
	send_all_to_buffers();

	// send buffers of shutdown thread
	send_thread_buffers(tld);

	// TODO ! suspend all *other* marked threads (they should no be in native code)
	// and send their buffers
	// you can stop them one by one using linux pid
	//   - pid id used instead of avail_thread_id as a thread id
	// resume threads after the sending thread is finished

	//jthread thread_obj;
	//jvmtiError error = (*jvmti_env)->GetCurrentThread(jvmti_env, &thread_obj);
	//check_jvmti_error(jvmti_env, error, "Cannot get object of current thread.");
	//GetAllThreads
	//SuspendThread
	//ResumeThread
	//GetThreadState

	// shutdown - first tagging then sending thread

	no_tagging_work = TRUE;

	// send empty buff to obj_tag thread -> ensures exit if waiting
	process_buffs * buffs = buffs_get(tld->id);
	buffs_objtag(buffs);

	// cleanup threads
	int rc1 = pthread_join(objtag_thread, NULL);
	check_error(rc1 != 0, "Cannot join tagging thread.");

	no_sending_work = TRUE;

	// TODO if multiple sending threads, multiple empty buffers have to be send
	// TODO also the buffers should be numbered according to the arrival to the
	// sending queue - has to be supported by the queue itself

	// send empty buff to obj_tag thread -> ensures exit if waiting
	buffs = buffs_get(tld->id);
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
	int marked_thread_count = 0;
	int non_marked_thread_count = 0;

	int i; // C99 needed for in cycle definition :)
	for(i = 0; i < BQ_BUFFERS; ++i) {

		// buffer held by thread that performed (is still doing) analysis
		//  - probably analysis data
		if(pb_list[i].owner_id >= STARTING_THREAD_ID) {
			relevant_count += buffer_filled(pb_list[i].analysis_buff);
			support_count += buffer_filled(pb_list[i].command_buff);
			++marked_thread_count;
#ifdef DEBUG
			printf("Lost buffer for id %ld\n", pb_list[i].owner_id);
#endif
		}

		// buffer held by thread that did NOT perform analysis
		//  - support data
		if(pb_list[i].owner_id == INVALID_THREAD_ID) {
			support_count += buffer_filled(pb_list[i].analysis_buff) +
					buffer_filled(pb_list[i].command_buff);
			++non_marked_thread_count;
		}

		check_error(pb_list[i].owner_id == PB_OBJTAG,
				"Unprocessed buffers left in object tagging queue");

		check_error(pb_list[i].owner_id == PB_SEND,
				"Unprocessed buffers left in sending queue");
	}

#ifdef DEBUG
	if(relevant_count > 0 || support_count > 0) {
		fprintf(stderr, "%s%s%d%s%d%s%s%d%s%d%s",
				"Warning: ",
				"Due to non-terminated (daemon) threads, ",
				relevant_count,
				" bytes of relevant data and ",
				support_count,
				" bytes of support data were lost ",
				"(thread count - analysis: ",
				marked_thread_count,
				", helper: ",
				non_marked_thread_count,
				").\n");
	}
#endif

	// NOTE: If we clean up, and daemon thread will use the structures,
	// it will crash. It is then better to leave it all as is.
	// dealloc buffers
	// cleanup blocking queues
	// cleanup java locks

#ifdef DEBUG
	printf("Shut down complete (thread %ld)\n", tld_get()->id);
#endif
}

// ******************* THREAD END callback *******************

void JNICALL jvmti_callback_thread_end_hook(
		jvmtiEnv *jvmti_env, JNIEnv* jni_env, jthread thread
) {
	// It should be safe to use thread locals according to jvmti documentation:
	// Thread end events are generated by a terminating thread after its initial
	// method has finished execution.

	// send all pending buffers associated with this thread
	send_thread_buffers(tld_get());
}

// ******************* JVMTI entry method *******************

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options, void *reserved) {

#ifdef DEBUG
	setvbuf(stdout, NULL, _IONBF, 0);
#endif

	//
	// Local initialization.
	//
	java_vm = jvm;
	tls_init ();

	//
	// First of all, get hold of a JVMTI interface version 1.0.
	// Failing to obtain the interface is a fatal error.
	//
	jvmti_env = NULL;
	jint res = (*jvm)->GetEnv(jvm, (void **) &jvmti_env, JVMTI_VERSION_1_0);
	if (res != JNI_OK || jvmti_env == NULL) {
		fprintf(stderr, 
				"%sUnable to access JVMTI Version 1 (0x%x),"
				" is your J2SE a 1.5 or newer version?"
				" JNIEnv's GetEnv() returned %d\n",
				ERR_PREFIX, JVMTI_VERSION_1, res
		);

		exit(ERR_JVMTI);
	}

	//
	// Request JVMTI capabilities:
	//
	//  - all class events
	//  - object tagging
	//  - object free notification
	//
	jvmtiCapabilities cap;
	memset(&cap, 0, sizeof(cap));
	cap.can_generate_all_class_hook_events = TRUE;
	cap.can_tag_objects = TRUE;
	cap.can_generate_object_free_events = TRUE;

	jvmtiError error;
	error = (*jvmti_env)->AddCapabilities(jvmti_env, &cap);
	check_jvmti_error(jvmti_env, error,
			"Unable to get necessary JVMTI capabilities.");

	// adding callbacks
	jvmtiEventCallbacks callbacks;
	memset(&callbacks, 0, sizeof(callbacks));

	callbacks.ClassFileLoadHook = &jvmti_callback_class_file_load_hook;
	callbacks.ObjectFree = &jvmti_callback_object_free_hook;
	callbacks.VMStart = &jvmti_callback_vm_start_hook;
	callbacks.VMInit = &jvmti_callback_vm_init_hook;
	callbacks.VMDeath = &jvmti_callback_vm_death_hook;
	callbacks.ThreadEnd = &jvmti_callback_thread_end_hook;

	error = (*jvmti_env)->SetEventCallbacks(
			jvmti_env, &callbacks, (jint) sizeof(callbacks)
	);
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

	error = (*jvmti_env)->CreateRawMonitor(jvmti_env, "buffids",
			&to_buff_lock);
	check_jvmti_error(jvmti_env, error, "Cannot create raw monitor");

	// read options (port/hostname)
	parse_agent_options(options);

	// init blocking queues
	bq_create(jvmti_env, &empty_buff_q, BQ_BUFFERS, sizeof(process_buffs *));
	bq_create(jvmti_env, &objtag_q, BQ_BUFFERS, sizeof(process_buffs *));
	bq_create(jvmti_env, &send_q, BQ_BUFFERS, sizeof(process_buffs *));

	// allocate buffers and add to the empty buffer queue
	int i;
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

	for(i = 0; i < TO_BUFFER_COUNT; ++i) {

		to_buff_array[i].pb = NULL;
	}

	return 0;
}

// ******************* REDispatch methods *******************

JNIEXPORT jshort JNICALL Java_ch_usi_dag_dislre_REDispatch_registerMethod
(JNIEnv * jni_env, jclass this_class, jstring analysis_method_desc) {

	return register_method(jni_env, analysis_method_desc, tld_get()->id);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_analysisStart__S
(JNIEnv * jni_env, jclass this_class, jshort analysis_method_id) {

	analysis_start(jni_env, analysis_method_id, tld_get());
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_analysisStart__SB
(JNIEnv * jni_env, jclass this_class, jshort analysis_method_id,
		jbyte ordering_id) {

	analysis_start_buff(jni_env, analysis_method_id, ordering_id, tld_get());
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_analysisEnd
(JNIEnv * jni_env, jclass this_class) {

	analysis_end(tld_get());
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendBoolean
(JNIEnv * jni_env, jclass this_class, jboolean to_send) {

	pack_boolean(tld_get()->analysis_buff, to_send);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendByte
(JNIEnv * jni_env, jclass this_class, jbyte to_send) {

	pack_byte(tld_get()->analysis_buff, to_send);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendChar
(JNIEnv * jni_env, jclass this_class, jchar to_send) {

	pack_char(tld_get()->analysis_buff, to_send);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendShort
(JNIEnv * jni_env, jclass this_class, jshort to_send) {

	pack_short(tld_get()->analysis_buff, to_send);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendInt
(JNIEnv * jni_env, jclass this_class, jint to_send) {

	pack_int(tld_get()->analysis_buff, to_send);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendLong
(JNIEnv * jni_env, jclass this_class, jlong to_send) {

	pack_long(tld_get()->analysis_buff, to_send);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendFloatAsInt
(JNIEnv * jni_env, jclass this_class, jint to_send) {

	pack_int(tld_get()->analysis_buff, to_send);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendDoubleAsLong
(JNIEnv * jni_env, jclass this_class, jlong to_send) {

	pack_long(tld_get()->analysis_buff, to_send);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendObject
(JNIEnv * jni_env, jclass this_class, jobject to_send) {

	struct tldata * tld = tld_get ();
	pack_object(jni_env, tld->analysis_buff, tld->command_buff, to_send,
			OT_OBJECT);
}

JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendObjectPlusData
(JNIEnv * jni_env, jclass this_class, jobject to_send) {

	struct tldata * tld = tld_get ();
	pack_object(jni_env, tld->analysis_buff, tld->command_buff, to_send,
			OT_DATA_OBJECT);
}
