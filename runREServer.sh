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

# start the remote execution server
# options available:
#	-Ddebug=true \
#	-Ddislreserver.port="portNum" \
${JAVA_HOME:+$JAVA_HOME/jre/bin/}java \
	-Xms1G -Xmx2G \
	-cp ${INSTR_LIB}:${DISL_LIB_P}/dislre-server.jar \
	ch.usi.dag.dislreserver.DiSLREServer \
	"$@" &

# print pid to the server file
if [ -n "${RE_SERVER_FILE}" ]; then
	echo $! > ${RE_SERVER_FILE}
fi
