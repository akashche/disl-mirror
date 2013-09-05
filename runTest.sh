#!/bin/bash
TARGET_BASE="ch.usi.dag.disl.test"
TARGET_MAIN="TargetClass"

if [ $# -lt 1 ]; then
	echo "Usage: `basename $0` <test-name>"
	echo "<test-name> is a package under $TARGET_BASE containing $TARGET_MAIN"
	exit 1
fi


kill_server () {
	if [ -f "$1" ]; then
		local PID=$(< "$1")
		[ -n "$PID" ] && kill -KILL $PID 2> /dev/null
		rm -f "$1"
	fi
}


#
# Compile the test package to make sure we have up-to-date jars.
# Bail out now if the compilation fails.
#
ant package-test -Dtest.name=$1 > /dev/null || exit 2


#
# Configure server PID files and mop-up any leftover
# instrumentation or remote/analysis servers.
#
export SERVER_FILE=.server.pid
kill_server $SERVER_FILE

export RE_SERVER_FILE=.re_server.pid
kill_server $RE_SERVER_FILE


#
# Launch the instrumentation and analysis servers and give them time to start.
# Then run the client with the class representing the observed program.
#
# Include the library containing the instrumentation in their class path.
#
INSTR_LIB=build/disl-instr.jar

./runServer.sh ${INSTR_LIB}
./runREServer.sh ${INSTR_LIB}

sleep 3

./runClient.sh ${INSTR_LIB} -cp bin/ "$TARGET_BASE.$1.$TARGET_MAIN"


#
# Give the servers some time to shut down themselves, before mopping up.
#
sleep 1

kill_server $SERVER_FILE
kill_server $RE_SERVER_FILE
