#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "common.h"


/**
 * Reports an actual general error. This function implements the slow path of
 * check_error(). It prints the given error message and exits with an error code
 * indicating a general error.
 */
void
report_error (const char * message) {
	fprintf (stderr, "%s%s\n", ERROR_PREFIX, message);
	exit (ERROR);
}


/**
 * Reports an actual standard library error. This function implements the slow path
 * of check_std_error(). It prints the given error message along with the error
 * message provided by the standard library, and exits with an error code indicating
 * failure in standard library call.
 */
void
report_std_error (const char * message) {
	char msgbuf [1024];

	snprintf (msgbuf, sizeof (msgbuf), "%s%s", ERROR_PREFIX, message);
	perror (msgbuf);
	exit (ERROR_STD);
}


/**
 * Returns the index of the given value in the given array of values.
 * Returns -1 if the value could not be found among the allowed values.
 */
int
find_value_index (const char * strval, const char * values [], int nvals) {
	for (int i = 0; i < nvals; i++) {
		if (strcasecmp (values [i], strval) == 0) {
			return i;
		}
	}

	return -1;
}

