# /bin/sh

EXPECTED_ARGS=1

if [ $# -lt $EXPECTED_ARGS ]
then
    echo "Usage: `basename $0` test-case"
    exit
fi

SERVER_FILE=.server.pid
export SERVER_FILE

RE_SERVER_FILE=re_server.pid
export RE_SERVER_FILE

# kill running server
if [ -e ${SERVER_FILE} ]
then
    kill -KILL `cat ${SERVER_FILE}`
    rm .server.pid
fi

# kill running server
if [ -e ${RE_SERVER_FILE} ]
then
    kill -KILL `cat ${RE_SERVER_FILE}`
    rm ${RE_SERVER_FILE}
fi


# represents the observed program
TARGET_CLASS="ch.usi.dag.disl.test.$1.TargetClass"

# compile the test package - suppress output
ant package-test -Dtest.name=$1 > /dev/null

INSTR_LIB=build/disl-instr.jar

# start server and take pid
./runServer.sh ${INSTR_LIB}

# start reserver and take pid
./runREServer.sh ${INSTR_LIB}

# wait for server startup
sleep 3

# run client
./runClient.sh ${INSTR_LIB} -cp bin/ ${TARGET_CLASS}

# wait for server shutdown
sleep 1

# kill server
kill -KILL `cat ${SERVER_FILE}` 2> /dev/null
rm .server.pid
kill -KILL `cat ${RE_SERVER_FILE}` 2> /dev/null
rm ${RE_SERVER_FILE}
