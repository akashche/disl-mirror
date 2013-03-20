#ifndef _COMMON_H_
#define _COMMON_H_

#include <stdio.h>
#include <stdbool.h>


#define ERROR 10000
#define ERROR_SERVER 10003
#define ERROR_STD 10002
#define ERROR_JVMTI 10003

#define ERROR_PREFIX "DiSL agent error: "


void report_error (const char * message);
void report_std_error (const char * message);


/**
 * Returns size of an array in array elements.
 */
#define sizeof_array(array)	\
		(sizeof (array) / sizeof ((array) [0]))


/**
 * Checks for a general error by testing the results of an error condition.
 * Reports a general error and terminates the program if the condition is true.
 */
inline static void
check_error (bool error, const char * message) {
	if (error) {
		report_error (message);
	}
}


/**
 * Checks the return value of a standard library call for error. Reports a standard
 * library error and terminates the program if the the returned value matches the
 * specified error value.
 */
inline static void
check_std_error (int retval, int errorval, const char * message) {
	if (retval == errorval) {
		report_std_error (message);
	}
}


int find_value_index (const char * strval, const char * values [], int nvals);

#endif /* _COMMON_H_ */
