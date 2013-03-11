#!/bin/sh

# set default lib path
if [ -z "${DISL_LIB_P}" ]; then
	DISL_LIB_P=./build
fi

# test number of arguments
if [ $# -lt 1 ]; then
	echo "Usage: `basename $0` instr-lib [java-params]"
	exit 1
fi

# get instrumentation library and shift parameters
INSTR_LIB=$1
shift

# start the instrumentation server
# options available:
#	-Ddebug=true \
#	-Ddisl.classes="list of disl classes (: - separator)"
#	-Ddisl.noexcepthandler=true \
#	-Ddisl.exclusionList="path" \
#	-Ddislserver.instrumented="path" \
#	-Ddislserver.uninstrumented="path" \
#	-Ddislserver.port="portNum" \
#	-Ddislserver.timestat=true \
#	-Ddislserver.continuous=true \
${JAVA_HOME:+$JAVA_HOME/jre/bin/}java \
	-cp ${INSTR_LIB}:${DISL_LIB_P}/disl-server.jar \
	ch.usi.dag.dislserver.DiSLServer \
	"$@" &

# print pid to the server file
if [ -n "${SERVER_FILE}" ]; then
	echo $! > ${SERVER_FILE}
fi
