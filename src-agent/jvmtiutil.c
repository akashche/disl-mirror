#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <jvmti.h>

#include "common.h"
#include "jvmtiutil.h"

#ifndef ERROR_PREFIX
#error ERROR_PREFIX macro has to be defined
#endif

#ifndef ERROR_JVMTI
#error ERROR_JVMTI macro has to be defined
#endif



bool
jvmti_redefine_class (
	jvmtiEnv * jvmti_env, JNIEnv * jni_env,
	const char * class_name, const jvmtiClassDefinition * class_def
) {
	assert (jni_env != NULL);
	assert (jvmti_env != NULL);
	assert (class_name != NULL);
	assert (class_def != NULL);

	jclass class = (* jni_env)->FindClass (jni_env, class_name);
	if (class == NULL) {
		return false;
	}

	//

	jvmtiClassDefinition new_classdef = * class_def;
	new_classdef.klass = class;

	jvmtiError error = (*jvmti_env)->RedefineClasses (jvmti_env, 1, &new_classdef);
	check_jvmti_error (jvmti_env, error, "failed to redefine class");

	return true;
}



static char *
__get_system_property (jvmtiEnv * jvmti, const char * name) {
	//
	// If the requested property does not exist, GetSystemProperty() will
	// return JVMTI_ERROR_NOT_AVAILABLE and will not modify the value pointer.
	// The other error that could occur is JVMTI_ERROR_NULL_POINTER, but we
	// assert that could not happen.
	//
	char * value = NULL;
	(*jvmti)->GetSystemProperty (jvmti, name, & value);

	if (value == NULL) {
		return NULL;
	}

	//

	char * result = strdup (value);
	check_error (result == NULL, "failed to duplicate system property value");

	jvmtiError error = (*jvmti)->Deallocate (jvmti, (unsigned char *) value);
	check_jvmti_error (jvmti, error, "failed to deallocate system property value");

	return result;
}



static bool
__parse_bool (const char * strval) {
	static const char * trues [] = { "true", "yes", "on", "1" };
	return find_value_index (strval, trues, sizeof_array (trues)) >= 0;
}


bool
jvmti_get_system_property_bool (
	jvmtiEnv * jvmti, const char * name, bool dflval
) {
	assert (jvmti != NULL);
	assert (name != NULL);

	char * strval = __get_system_property (jvmti, name);
	if (strval != NULL) {
		bool result = __parse_bool (strval);
		free (strval);

		return result;

	} else {
		return dflval;
	}
}


char *
jvmti_get_system_property_string (
	jvmtiEnv * jvmti, const char * name, const char * dflval
) {
	assert (jvmti != NULL);
	assert (name != NULL);

	char * strval = __get_system_property (jvmti, name);
	if (strval != NULL) {
		return strval;

	} else if (dflval != NULL) {
		//
		// Duplicate the default value so that the caller always "owns"
		// the returned value and can release it using free().
		//
		char * result = strdup (dflval);
		check_error (result == NULL, "failed to duplicate property value");
		return result;

	} else {
		return NULL;
	}
}


/**
 * Reports an actual JVMTI error. This function implements the slow path of
 * check_jvmti_error() and prints the given error message along with a JVMTI error
 * name obtained using the GetErrorName() JVMTI interface.
 */
void
report_jvmti_error (jvmtiEnv *jvmti, jvmtiError errnum, const char *str) {
	char * errnum_str = NULL;
	(void) (*jvmti)->GetErrorName (jvmti, errnum, &errnum_str);

	fprintf (
		stderr, "%sJVMTI: %d (%s): %s\n",
		ERROR_PREFIX, errnum,
		(errnum_str == NULL ? "Unknown" : errnum_str),
		(str == NULL ? "" : str)
	);

	exit (ERROR_JVMTI);
}
