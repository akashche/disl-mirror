# /bin/sh

EXPECTED_ARGS=1

if [ $# -lt $EXPECTED_ARGS ]
then
    echo "Usage: `basename $0` test-case [pkg]"
    exit
fi

DISL_CLASS="./bin/ch/usi/dag/disl/test/$1/DiSLClass.class"
TARGET_CLASS="ch.usi.dag.disl.test.$1.TargetClass"

if [ "$2" == "pkg" ]
then
    # start server and take pid
    ant package -Dtest.name=$1
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
kill -KILL `cat .server.pid`
rm .server.pid
