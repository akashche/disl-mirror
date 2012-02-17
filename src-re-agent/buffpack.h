#ifndef _BUFFPACK_H
#define	_BUFFPACK_H

void pack_boolean(buffer * buff, jboolean to_send) {

	buffer_fill(buff, &to_send, sizeof(jboolean));
}

void pack_byte(buffer * buff, jbyte to_send) {

	buffer_fill(buff, &to_send, sizeof(jbyte));
}

void pack_char(buffer * buff, jchar to_send) {

	jchar nts = htons(to_send);
	buffer_fill(buff, &nts, sizeof(jchar));
}

void pack_short(buffer * buff, jshort to_send) {

	jshort nts = htons(to_send);
	buffer_fill(buff, &nts, sizeof(jshort));
}

void pack_int(buffer * buff, jint to_send) {

	jint nts = htonl(to_send);
	buffer_fill(buff, &nts, sizeof(jint));
}

void pack_long(buffer * buff, jlong to_send) {

	jlong nts = htobe64(to_send);
	buffer_fill(buff, &nts, sizeof(jlong));
}

void pack_string_utf8(buffer * buff, const void * string_utf8,
		uint16_t size_in_bytes) {

	// send length first
	uint16_t nsize = htons(size_in_bytes);
	buffer_fill(buff, &nsize, sizeof(uint16_t));

	buffer_fill(buff, string_utf8, size_in_bytes);
}

void pack_string_java(buffer * buff, jstring to_send, JNIEnv * jni_env) {

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

void pack_object(buffer * buff, jobject to_send, JNIEnv * jni_env) {

	jlong obj_tag;

	enter_critical_section(jvmti_env, objectid_lock);
	{

		jvmtiError error;

		// get object tag
		error = (*jvmti_env)->GetTag(jvmti_env, to_send, &obj_tag);
		check_jvmti_error(jvmti_env, error, "Cannot get object tag");

		// set object tag
		if(obj_tag == 0) {

			obj_tag = avail_object_tag;
			++avail_object_tag;

			// TODO add class id - note that class can miss the class id

			error = (*jvmti_env)->SetTag(jvmti_env, to_send, obj_tag);
			check_jvmti_error(jvmti_env, error, "Cannot set object tag");
		}

	}
	exit_critical_section(jvmti_env, objectid_lock);

	pack_long(buff, obj_tag);
}

void pack_class(buffer * buff, jclass to_send, JNIEnv * jni_env) {

	// TODO
	// class id is set for jclass on the same spot as for object
	// class id can have object id also

	// TODO
	// if class does not have id, you have to find it by name and class loader

	pack_int(buff, 1);
}

void pack_bytes(buffer * buff, const void * data, jint size) {

	buffer_fill(buff, data, size);
}

#endif	/* _BUFFPACK_H */
