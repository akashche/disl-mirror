# /bin/sh

EXPECTED_ARGS=1

if [ $# -gt $EXPECTED_ARGS ]
then
    echo "Usage: `basename $0` [pkg]"
    exit
fi

SERVER_FILE=.server.pid
export SERVER_FILE

if [ -e ${SERVER_FILE} ]
then
    kill -KILL `cat ${SERVER_FILE}`
    rm .server.pid
fi

DISL_CLASS="./bin/ch/usi/dag/disl/example/jcarder/DiSLClass.class"
TARGET_CLASS="ch.usi.dag.disl.example.jcarder.TargetClass"

if [ "$1" = "pkg" ]
then
    # start server and take pid
    ant package -Dtest.name=jcarder
    ./runServer.sh
else
    # start server and take pid
    ./runServer.sh -Ddisl.classes=${DISL_CLASS}
fi

# wait for server startup
sleep 5

# run client
./runClient.sh ${TARGET_CLASS}

# kill server
kill -KILL `cat ${SERVER_FILE}`
rm .server.pid
