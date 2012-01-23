# /bin/sh

EXPECTED_ARGS=1

if [ $# -lt $EXPECTED_ARGS ]
then
    echo "Usage: `basename $0` test-case"
    exit
fi

SERVER_FILE=.server.pid
export SERVER_FILE

# kill running server
if [ -e ${SERVER_FILE} ]
then
    kill -KILL `cat ${SERVER_FILE}`
    rm .server.pid
fi

DISL_CLASS="./bin/ch/usi/dag/disl/test/$1/DiSLClass.class"
TARGET_CLASS="ch.usi.dag.disl.test.$1.TargetClass"

# start server and take pid
./runServer.sh -Ddisl.classes=${DISL_CLASS}

# wait for server startup
sleep 3

# run client
./runClient.sh ${TARGET_CLASS}

# wait for server shutdown
sleep 1

# kill server
kill -KILL `cat ${SERVER_FILE}` 2> /dev/null
rm .server.pid
