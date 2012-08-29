#!/bin/bash

# set default lib path
if [ -z "${DISL_LIB_P}" ]; then
	DISL_LIB_P=./build
fi

# test number of arguments
EXPECTED_ARGS=2
if [ $# -lt $EXPECTED_ARGS ]
then
	echo "Usage: `basename $0` instr-lib java-params"
	exit
fi

# set proper lib depending on OS
OS=`uname`
if [ "${OS}" = "Darwin" ]; then
	C_AGENT="${DISL_LIB_P}/libdislagent.jnilib"
	RE_AGENT="${DISL_LIB_P}/libdislreagent.jnilib"
else
	C_AGENT="${DISL_LIB_P}/libdislagent.so"
	RE_AGENT="${DISL_LIB_P}/libdislreagent.so"
fi

# get instrumentation library and shift parameters
INSTR_LIB=$1
shift

# start client
java -agentpath:${C_AGENT} \
     -agentpath:${RE_AGENT} \
     -javaagent:${DISL_LIB_P}/disl-agent.jar \
     -Xbootclasspath/a:${DISL_LIB_P}/disl-agent.jar:${INSTR_LIB}:${DISL_LIB_P}/dislre-dispatch.jar \
      $*
