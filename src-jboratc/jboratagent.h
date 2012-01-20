#include <jvmti.h>

#ifndef _JBORATCLIENTAGENT_H
#define	_JBORATCLIENTAGENT_H

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jint JNICALL Agent_OnLoad(JavaVM *jvm, char *options, void *reserved);

#ifdef __cplusplus
}
#endif


#endif	/* _JBORATCLIENTAGENT_H */
