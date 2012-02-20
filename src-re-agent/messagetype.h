#ifndef _MESSAGETYPE_H
#define	_MESSAGETYPE_H

// Messages Types
//  - should be in sync with java server

// closing connection
static const jint MSG_CLOSE = 0;
// sending analysis
static const jint MSG_ANALYZE = 1;
// sending object free
static const jint MSG_OBJ_FREE = 2;
// sending new class
static const jint MSG_NEW_CLASS = 3;

#endif	/* _MESSAGETYPE_H */
