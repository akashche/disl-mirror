#!/bin/bash

# set default lib path
if [ -z "${DISL_LIB_P}" ]; then
	DISL_LIB_P=./build
fi

# test number of arguments
if [ $# -lt 2 ]; then
	echo "Usage: `basename $0` instr-lib java-params"
	exit 1
fi

# determine libs depending on the OS
OS=`uname`
if [ "${OS}" = "Darwin" ]; then
	AGENT="${DISL_LIB_P}/libdislagent.jnilib"
	RE_AGENT="${DISL_LIB_P}/libdislreagent.jnilib"
else
	AGENT="${DISL_LIB_P}/libdislagent.so"
	RE_AGENT="${DISL_LIB_P}/libdislreagent.so"
fi

# get instrumentation library and shift parameters
INSTR_LIB=$1
shift

# start the client
# options available:
#	-Ddisl.bypass=never \
#	-Ddisl.bypass=bootstrap \
#	-Ddisl.bypass=dynamic \
#	-Ddisl.splitmethods=false \
#	-Ddisl.excepthandler=true \
${JAVA_HOME:+$JAVA_HOME/jre/bin/}java \
	-agentpath:${AGENT} -agentpath:${RE_AGENT} \
	-Xbootclasspath/a:${DISL_LIB_P}/disl-bypass.jar:${INSTR_LIB}:${DISL_LIB_P}/dislre-dispatch.jar \
	"$@"
