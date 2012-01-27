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

java -agentpath:${JBORAT_AGENT}=${JBORAT_AGENT_OPTS} \
     -javaagent:build/dislagent-unspec.jar \
     -Xbootclasspath/a:build/dislagent-unspec.jar:build/dislinstr.jar \
     -cp bin/ \
      $*
