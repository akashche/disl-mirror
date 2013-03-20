#ifndef _BYTECODE_H_
#define _BYTECODE_H_

#include <jvmti.h>


/**
 * Name of the class used for bypass checks.
 */
#define BPC_CLASS_NAME "ch/usi/dag/disl/dynamicbypass/BypassCheck"


/**
 * Externs for various implementations of the BypassCheck class.
 */
extern jvmtiClassDefinition bpc_always_classdef;
extern jvmtiClassDefinition bpc_dynamic_classdef;
extern jvmtiClassDefinition bpc_never_classdef;

#endif /* _BYTECODE_H_ */
