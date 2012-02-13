#!/bin/bash

OS=`uname`

if [ "${OS}" = "Darwin" ]; then
  C_AGENT="src-agent-c/libdislagent.jnilib"
else
  C_AGENT="src-agent-c/libdislagent.so"
fi

# ipc.socket is the default communication
# to enable shared memory, remove ",ipc.socket" from the options
java -agentpath:${C_AGENT} \
     -javaagent:build/dislagent-unspec.jar \
     -Xbootclasspath/a:build/dislagent-unspec.jar:build/dislinstr.jar \
     -cp bin/ \
      $*
