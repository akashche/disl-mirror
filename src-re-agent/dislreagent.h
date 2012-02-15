#include <jvmti.h>

#ifndef _DISLAGENT_H
#define	_DISLAGENT_H

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options, void *reserved);

#ifdef __cplusplus
}
#endif

// ******************* REDispatch methods *******************

#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     ch_usi_dag_dislre_REDispatch
 * Method:    analyse
 * Signature: (II[Ljava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_ch_usi_dag_dislre_REDispatch_analyse
  (JNIEnv *, jclass, jint, jint, jobjectArray);

#ifdef __cplusplus
}
#endif

#endif	/* _DISLAGENT_H */
