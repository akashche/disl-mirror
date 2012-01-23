#!/bin/bash

OS=`uname`

if [ "${OS}" = "Darwin" ]; then
  JBORAT_AGENT="src-agent-c/libdislagent.jnilib"
else
  JBORAT_AGENT="src-agent-c/libdislagent.so"
fi

# ipc.socket is the default communication
# to enable shared memory, remove ",ipc.socket" from the options
JBORAT_AGENT_OPTS="1234,localhost,ipc.socket"

java -XX:MaxPermSize=128m \
    -Xmx2G \
    -javaagent:build/dislagent-unspec.jar \
    -Xbootclasspath/p:build/dislagent-unspec.jar \
    -agentpath:${JBORAT_AGENT}=${JBORAT_AGENT_OPTS} \
    -cp ./bin \
     $*
