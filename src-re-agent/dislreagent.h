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
 * Method:    analysisStart
 * Signature: (Ljava/lang/String;)I
 */
JNIEXPORT jint JNICALL Java_ch_usi_dag_dislre_REDispatch_analysisStart
  (JNIEnv *, jclass, jstring);

/*
 * Class:     ch_usi_dag_dislre_REDispatch
 * Method:    analysisEnd
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_analysisEnd
  (JNIEnv *, jclass, jint);

/*
 * Class:     ch_usi_dag_dislre_REDispatch
 * Method:    sendBoolean
 * Signature: (IZ)V
 */
JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendBoolean
  (JNIEnv *, jclass, jint, jboolean);

/*
 * Class:     ch_usi_dag_dislre_REDispatch
 * Method:    sendByte
 * Signature: (IB)V
 */
JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendByte
  (JNIEnv *, jclass, jint, jbyte);

/*
 * Class:     ch_usi_dag_dislre_REDispatch
 * Method:    sendChar
 * Signature: (IC)V
 */
JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendChar
  (JNIEnv *, jclass, jint, jchar);

/*
 * Class:     ch_usi_dag_dislre_REDispatch
 * Method:    sendShort
 * Signature: (IS)V
 */
JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendShort
  (JNIEnv *, jclass, jint, jshort);

/*
 * Class:     ch_usi_dag_dislre_REDispatch
 * Method:    sendInt
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendInt
  (JNIEnv *, jclass, jint, jint);

/*
 * Class:     ch_usi_dag_dislre_REDispatch
 * Method:    sendLong
 * Signature: (IJ)V
 */
JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendLong
  (JNIEnv *, jclass, jint, jlong);

/*
 * Class:     ch_usi_dag_dislre_REDispatch
 * Method:    sendFloatAsInt
 * Signature: (II)V
 */
JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendFloatAsInt
  (JNIEnv *, jclass, jint, jint);

/*
 * Class:     ch_usi_dag_dislre_REDispatch
 * Method:    sendDoubleAsLong
 * Signature: (IJ)V
 */
JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendDoubleAsLong
  (JNIEnv *, jclass, jint, jlong);

/*
 * Class:     ch_usi_dag_dislre_REDispatch
 * Method:    sendString
 * Signature: (ILjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendString
  (JNIEnv *, jclass, jint, jstring);

/*
 * Class:     ch_usi_dag_dislre_REDispatch
 * Method:    sendObject
 * Signature: (ILjava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendObject
  (JNIEnv *, jclass, jint, jobject);

/*
 * Class:     ch_usi_dag_dislre_REDispatch
 * Method:    sendClass
 * Signature: (ILjava/lang/Class;)V
 */
JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_sendClass
  (JNIEnv *, jclass, jint, jclass);

#ifdef __cplusplus
}
#endif

#endif	/* _DISLAGENT_H */
