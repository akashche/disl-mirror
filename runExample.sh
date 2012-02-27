# /bin/sh

EXPECTED_ARGS=1

if [ $# -lt $EXPECTED_ARGS ]
then
    echo "Usage: `basename $0` <test-case> [version]"
    exit
fi

SERVER_FILE=server.pid
export SERVER_FILE

# kill running server
if [ -e ${SERVER_FILE} ]
then
    kill -KILL `cat ${SERVER_FILE}`
    kill -KILL `cat re_${SERVER_FILE}` 2> /dev/null
    rm ${SERVER_FILE}
    rm re_${SERVER_FILE}
fi

if [ $# -eq 2 ]
then
    exampleManifest="MANIFEST_$2.MF"
else
    exampleManifest="MANIFEST.MF"
fi

DISL_CLASS="./bin/ch/usi/dag/disl/example/$1/DiSLClass.class"
TARGET_CLASS="ch.usi.dag.disl.example.$1.TargetClass"

# start server and take pid
# suppress output
ant package-example -Dexample.name=$1 -Dexample.manifest=${exampleManifest} > /dev/null
./runServer.sh
./runREServer.sh

# wait for server startup
sleep 3

# run client
./runClient.sh ${TARGET_CLASS}

# wait for server shutdown
sleep 1

# kill servers
kill -KILL `cat ${SERVER_FILE}` 2> /dev/null
kill -KILL `cat re_${SERVER_FILE}` 2> /dev/null
rm ${SERVER_FILE}
rm re_${SERVER_FILE}
