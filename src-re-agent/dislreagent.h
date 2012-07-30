#include <jvmti.h>

#ifndef _DISLAGENT_H
#define	_DISLAGENT_H

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options, void *reserved);

#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     ch_usi_dag_dislre_REDispatch
 * Method:    registerMethod
 * Signature: (Ljava/lang/String;)S
 */
JNIEXPORT jshort JNICALL Java_ch_usi_dag_dislre_REDispatch_registerMethod
  (JNIEnv *, jclass, jstring);

/*
 * Class:     ch_usi_dag_dislre_REDispatch
 * Method:    analysisStart
 * Signature: (S)V
 */
JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_analysisStart
  (JNIEnv *, jclass, jshort);

/*
 * Class:     ch_usi_dag_dislre_REDispatch
 * Method:    analysisEnd
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_analysisEnd
  (JNIEnv *, jclass);

/*
 * Class:     ch_usi_dag_dislre_REDispatch
 * Method:    sendBoolean
 * Signature: (Z)V
 */
JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendBoolean
  (JNIEnv *, jclass, jboolean);

/*
 * Class:     ch_usi_dag_dislre_REDispatch
 * Method:    sendByte
 * Signature: (B)V
 */
JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendByte
  (JNIEnv *, jclass, jbyte);

/*
 * Class:     ch_usi_dag_dislre_REDispatch
 * Method:    sendChar
 * Signature: (C)V
 */
JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendChar
  (JNIEnv *, jclass, jchar);

/*
 * Class:     ch_usi_dag_dislre_REDispatch
 * Method:    sendShort
 * Signature: (S)V
 */
JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendShort
  (JNIEnv *, jclass, jshort);

/*
 * Class:     ch_usi_dag_dislre_REDispatch
 * Method:    sendInt
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendInt
  (JNIEnv *, jclass, jint);

/*
 * Class:     ch_usi_dag_dislre_REDispatch
 * Method:    sendLong
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendLong
  (JNIEnv *, jclass, jlong);

/*
 * Class:     ch_usi_dag_dislre_REDispatch
 * Method:    sendFloatAsInt
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendFloatAsInt
  (JNIEnv *, jclass, jint);

/*
 * Class:     ch_usi_dag_dislre_REDispatch
 * Method:    sendDoubleAsLong
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendDoubleAsLong
  (JNIEnv *, jclass, jlong);

/*
 * Class:     ch_usi_dag_dislre_REDispatch
 * Method:    sendString
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendString
  (JNIEnv *, jclass, jstring);

/*
 * Class:     ch_usi_dag_dislre_REDispatch
 * Method:    sendObject
 * Signature: (Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendObject
  (JNIEnv *, jclass, jobject);

/*
 * Class:     ch_usi_dag_dislre_REDispatch
 * Method:    sendClass
 * Signature: (Ljava/lang/Class;)V
 */
JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendClass
  (JNIEnv *, jclass, jclass);

#ifdef __cplusplus
}
#endif

#endif	/* _DISLAGENT_H */
