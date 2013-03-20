#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <netdb.h>
#include <unistd.h>
#include <netinet/tcp.h>

#include <pthread.h>

#include <jvmti.h>
#include <jni.h>

#include "common.h"
#include "jvmtiutil.h"

#include "dislagent.h"
#include "connection.h"

#include "network.h"
#include "msgchannel.h"
#include "bytecode.h"
#include "codeflags.h"


// ****************************************************************************
// AGENT CONFIG
// ****************************************************************************

#define DISL_HOST_DEFAULT "localhost"
#define DISL_PORT_DEFAULT "11217"

#define DISL_BYPASS "disl.bypass"
#define DISL_BYPASS_DEFAULT "dynamic"

#define DISL_SPLIT_METHODS "disl.splitmethods"
#define DISL_SPLIT_METHODS_DEFAULT true

#define DISL_CATCH_EXCEPTIONS "disl.excepthandler"
#define DISL_CATCH_EXCEPTIONS_DEFAULT false

#define DISL_DEBUG "debug"
#define DISL_DEBUG_DEFAULT false


/**
 * The instrumentation bypass mode.
 */
enum bypass_mode {
	/**
	 * The original method code will not be preserved, therefore the
	 * instrumentation will never be bypassed.
	 */
	BYPASS_MODE_NEVER = 0,

	/**
	 * The original method code will be preserved and used instead of
	 * the instrumented code during JVM bootstrap.
	 */
	BYPASS_MODE_BOOTSTRAP = 1,

	/**
	 * The original method code will be preserved and used instead of
	 * the instrumented code when inside the instrumentation.
	 */
	BYPASS_MODE_DYNAMIC = 2
};


/**
 * Flags representing code options, derived from the values generated from Java.
 */
enum code_flags {
	CF_CREATE_BYPASS = ch_usi_dag_disl_DiSL_CodeOption_Flag_CREATE_BYPASS,
	CF_DYNAMIC_BYPASS = ch_usi_dag_disl_DiSL_CodeOption_Flag_DYNAMIC_BYPASS,
	CF_SPLIT_METHODS = ch_usi_dag_disl_DiSL_CodeOption_Flag_SPLIT_METHODS,
	CF_CATCH_EXCEPTIONS = ch_usi_dag_disl_DiSL_CodeOption_Flag_CATCH_EXCEPTIONS,
};


struct config {
	char * host_name;
	char * port_number;

	enum bypass_mode bypass_mode;
	bool split_methods;
	bool catch_exceptions;

	bool debug;
};


/**
 * Agent configuration.
 */
static struct config agent_config;


/**
 * Code option flags that control the instrumentation.
 */
static volatile jint agent_code_flags;


// ****************************************************************************
// CLASS FILE LOAD
// ****************************************************************************

static jint
__calc_code_flags (struct config * config, bool jvm_is_booting) {
	jint result = 0;

	//
	// If bypass is desired, always create bypass code when the JVM is
	// bootstrapping. If dynamic bypass is desired, create bypass code as
	// well as code to control it dynamically.
	//
	if (config->bypass_mode > BYPASS_MODE_NEVER) {
		result |= jvm_is_booting ? CF_CREATE_BYPASS : 0;
		if (config->bypass_mode > BYPASS_MODE_BOOTSTRAP) {
			result |= (CF_CREATE_BYPASS | CF_DYNAMIC_BYPASS);
		}
	}

	result |= config->split_methods ? CF_SPLIT_METHODS : 0;
	result |= config->catch_exceptions ? CF_CATCH_EXCEPTIONS : 0;

	return result;
}


/**
 * Sends the given class to the remote server for instrumentation. Returns
 * the response from the server that contains the instrumented class.
 */
static struct message
__instrument_class (
	jint request_flags, const char * classname,
	const unsigned char * classcode, jint classcode_size
) {
	//
	// Acquire a connection, put the class data into the message and
	// send it to the server, wait for the response, and release the
	// connection again.
	//
	struct connection * conn = network_acquire_connection ();

	// TODO: This would do just with a thread-local message
	struct message request = create_message (
		request_flags,
		(const unsigned char *) classname, strlen (classname),
		classcode, classcode_size
	);

	send_message (conn, &request);

	struct message result = recv_message (conn);

	network_release_connection (conn);
	return result;
}


static void JNICALL
jvmti_callback_class_file_load (
	jvmtiEnv * jvmti, JNIEnv * jni,
	jclass class_being_redefined, jobject loader,
	const char * class_name, jobject protection_domain,
	jint class_data_len, const unsigned char * class_data,
	jint * new_class_data_len, unsigned char ** new_class_data
) {
#ifdef DEBUG
	if (class_name != NULL) {
		printf ("Instrumenting class %s\n", class_name);
	} else {
		printf ("Instrumenting unknown class\n");
	}
#endif

	// skip instrumentation of the bypass check class
	if (strcmp (class_name, BPC_CLASS_NAME) == 0) {
#ifdef DEBUG
		printf ("Skipping class %s\n", class_name);
#endif
		return;
	}


	// ask the server to instrument the class
	struct message instrclass = __instrument_class (
		agent_code_flags, class_name, class_data, class_data_len
	);

	// error on the server
	if (instrclass.control_size > 0) {
		// classname contains the error message
		fprintf(stderr, "%sError occurred in the remote instrumentation server\n", ERROR_PREFIX);
		fprintf(stderr, "   Reason: %s\n", instrclass.control);
		exit (ERROR_SERVER);
	}

	// instrumented class received (0 - means no instrumentation done)
	if(instrclass.classcode_size > 0) {
		// give to JVM the instrumented class
		unsigned char * new_class_space;

		// let JVMTI to allocate the mem for the new class
		jvmtiError err = (*jvmti)->Allocate (jvmti, (jlong) instrclass.classcode_size, & new_class_space);
		check_jvmti_error (jvmti, err, "Cannot allocate memory for the instrumented class");

		memcpy (new_class_space, instrclass.classcode, instrclass.classcode_size);

		// set the newly instrumented class + len
		*(new_class_data_len) = instrclass.classcode_size;
		*(new_class_data) = new_class_space;

		// free memory
		free_message (&instrclass);
	}

#ifdef DEBUG
	printf("Instrumentation done\n");
#endif
}


// ****************************************************************************
// JVMTI EVENT: VM START
// ****************************************************************************

static void JNICALL
jvmti_callback_vm_init (jvmtiEnv * jvmti, JNIEnv * jni, jthread thread) {
	//
	// Update code flags to reflect that the VM has stopped booting.
	//
	agent_code_flags = __calc_code_flags (& agent_config, false);

	//
	// Redefine the bypass check class. If dynamic bypass is required, use
	// a class that honors the dynamic bypass state for the current thread.
	// Otherwise use a class that disables bypassing instrumented code.
	//
	jvmtiClassDefinition * bpc_classdef;
	if (agent_config.bypass_mode == BYPASS_MODE_DYNAMIC) {
#ifdef DEBUG
			fprintf (stderr, "vm_init: redefining BypassCheck for dynamic bypass\n");
#endif
			bpc_classdef = & bpc_dynamic_classdef;
	} else {
#ifdef DEBUG
			fprintf (stderr, "vm_init: redefining BypassCheck to disable bypass\n");
#endif
			bpc_classdef = & bpc_never_classdef;
	}

	jvmti_redefine_class (jvmti, jni, BPC_CLASS_NAME, bpc_classdef);
}


// ****************************************************************************
// JVMTI EVENT: VM DEATH
// ****************************************************************************

static void JNICALL
jvmti_callback_vm_death (jvmtiEnv * jvmti, JNIEnv * jni) {
	//
	// Just close all the connections.
	//
	network_fini ();
}


// ****************************************************************************
// AGENT ENTRY POINT: ON LOAD
// ****************************************************************************

static void
__configure_from_properties (jvmtiEnv * jvmti, struct config * config) {
	//
	// Get bypass mode configuration
	//
	char * bypass = jvmti_get_system_property_string (
		jvmti, DISL_BYPASS, DISL_BYPASS_DEFAULT
	);

	static const char * values [] = { "never", "bootstrap", "dynamic" };
	int bypass_index = find_value_index (bypass, values, sizeof_array (values));
	check_error (bypass_index < 0, "invalid bypass mode, check " DISL_BYPASS);

	config->bypass_mode = bypass_index;
	free (bypass);


	//
	// Get boolean values from system properties
	//
	config->split_methods = jvmti_get_system_property_bool (
		jvmti, DISL_SPLIT_METHODS, DISL_SPLIT_METHODS_DEFAULT
	);

	config->catch_exceptions = jvmti_get_system_property_bool (
		jvmti, DISL_CATCH_EXCEPTIONS, DISL_CATCH_EXCEPTIONS_DEFAULT
	);

	config->debug = jvmti_get_system_property_bool (
		jvmti, DISL_DEBUG, DISL_DEBUG_DEFAULT
	);
}


static void
__configure_from_options (const char * options, struct config * config) {
	//
	// Assign default host name and port and bail out
	// if there are no agent options.
	//
	if (options == NULL) {
		config->host_name = strdup (DISL_HOST_DEFAULT);
		config->port_number = strdup (DISL_PORT_DEFAULT);
		return;
	}

	//
	// Parse the host name and port of the remote server.
	// Look for port specification first, then take the prefix
	// before ':' as the host name.
	//
	char * host_start = strdup (options);
	char * port_start = strchr (host_start, ':');
	if (port_start != NULL) {
		//
		// Split the option string at the port delimiter (':')
		// using an end-of-string character ('\0') and copy
		// the port.
		//
		port_start [0] = '\0';
		port_start++;
		
		config->port_number = strdup (port_start);
	}

	config->host_name = strdup (host_start);
}


static jvmtiEnv *
__acquire_jvmti (JavaVM * jvm) {
	jvmtiEnv * jvmti = NULL;

	jint result = (*jvm)->GetEnv (jvm, (void **) &jvmti, JVMTI_VERSION_1_0);
	if (result != JNI_OK || jvmti == NULL) {
		//
		// The VM was unable to provide the requested version of the
		// JVMTI interface. This is a fatal error for the agent.
		//
		fprintf (
			stderr,
			"%sFailed to obtain JVMTI interface Version 1 (0x%x)\n"
			"JVM GetEnv() returned %d - is your Java runtime "
			"version 1.5 or newer?\n",
			ERROR_PREFIX, JVMTI_VERSION_1, result
		);

		exit (ERROR_JVMTI);
	}

	return jvmti;
}


#ifdef WHOLE
#define VISIBLE __attribute__((externally_visible))
#else
#define VISIBLE
#endif


JNIEXPORT jint JNICALL VISIBLE
Agent_OnLoad (JavaVM * jvm, char * options, void * reserved) {
	jvmtiEnv * jvmti = __acquire_jvmti (jvm);

	// add capabilities
	jvmtiCapabilities cap;
	memset (&cap, 0, sizeof (cap));
	cap.can_redefine_classes = 1;
	cap.can_redefine_any_class = 1;
	cap.can_generate_all_class_hook_events = 1;

	jvmtiError error = (*jvmti)->AddCapabilities (jvmti, &cap);
	check_jvmti_error (jvmti, error, "Unable to get necessary JVMTI capabilities.");


	// register callbacks
	jvmtiEventCallbacks callbacks;
	(void) memset (&callbacks, 0, sizeof (callbacks));

	callbacks.VMInit = &jvmti_callback_vm_init;
	callbacks.VMDeath = &jvmti_callback_vm_death;
	callbacks.ClassFileLoadHook = &jvmti_callback_class_file_load;

	error = (*jvmti)->SetEventCallbacks (jvmti, &callbacks, (jint) sizeof (callbacks));
	check_jvmti_error (jvmti, error, "failed to register JVMTI event callbacks");


	// enable event notification
	error = (*jvmti)->SetEventNotificationMode (jvmti, JVMTI_ENABLE, JVMTI_EVENT_VM_INIT, NULL);
	check_jvmti_error (jvmti, error, "failed to enable VM INIT event");

	error = (*jvmti)->SetEventNotificationMode (jvmti, JVMTI_ENABLE, JVMTI_EVENT_VM_DEATH, NULL);
	check_jvmti_error (jvmti, error, "failed to enable VM DEATH event");

	error = (*jvmti)->SetEventNotificationMode (jvmti, JVMTI_ENABLE, JVMTI_EVENT_CLASS_FILE_LOAD_HOOK, NULL);
	check_jvmti_error (jvmti, error, "failed to enable CLASS FILE LOAD event");


	// configure agent and init connections
	__configure_from_options (options, & agent_config);
	__configure_from_properties (jvmti, & agent_config);

	agent_code_flags = __calc_code_flags (& agent_config, true);
	network_init (agent_config.host_name, agent_config.port_number);

	return 0;
}
