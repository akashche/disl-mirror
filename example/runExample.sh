# /bin/sh

EXPECTED_ARGS=1

if [ $# -lt $EXPECTED_ARGS ]
then
    echo "Usage: `basename $0` <test-case> [manifest file name]"
    exit
fi

CURR_PATH=`pwd`

SERVER_FILE="${CURR_PATH}/../server.pid"
export SERVER_FILE

RE_SERVER_FILE="${CURR_PATH}/../re_server.pid"
export RE_SERVER_FILE

# kill running server
if [ -e ${SERVER_FILE} ]
then
    kill -KILL `cat ${SERVER_FILE}`
    rm ${SERVER_FILE}
fi

# kill running re_server
if [ -e ${RE_SERVER_FILE} ]
then
    kill -KILL `cat ${RE_SERVER_FILE}`
    rm ${RE_SERVER_FILE}
fi

if [ $# -eq 2 ]
then
    exampleMF=$2
else
    exampleMF="MANIFEST.MF"
fi

TARGET_CLASS="TargetClass"

# start server and take pid
# suppress output
ant package-example -Dexample.name=$1 -Dexample.manifest=${exampleMF} #> /dev/null
../runServer.sh
../runREServer.sh

# wait for server startup
sleep 3

# run client
../runClient.sh ${TARGET_CLASS}

# wait for server shutdown
sleep 1

# kill server
kill -KILL `cat ${SERVER_FILE}` 2> /dev/null
rm ${SERVER_FILE}
# kill re_server
kill -KILL `cat ${RE_SERVER_FILE}` 2> /dev/null
rm ${RE_SERVER_FILE}
