# /bin/sh

# default DiSL lib path
if [ -z "${DISL_LIB_P}" ]; then
    DISL_LIB_P=../build
fi

# test number of arguments
EXPECTED_ARGS=2
if [ $# -lt $EXPECTED_ARGS ]
then
	echo "Usage: `basename $0` instr-lib java-params"
	exit
fi

# set server file
SERVER_FILE=.server.pid
export SERVER_FILE

# kill running server
if [ -e ${SERVER_FILE} ]
then
	kill -KILL `cat ${SERVER_FILE}`
	rm .server.pid
fi

export DISL_LIB_P

# start server (with instrumentation library) and take pid
./runServer.sh $1

# wait for server startup
sleep 3

# run client
./runClient.sh $*

# wait for server shutdown
sleep 1

# kill server
kill -KILL `cat ${SERVER_FILE}` 2> /dev/null
rm .server.pid
