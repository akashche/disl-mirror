#!/bin/bash

OS=`uname`

if [ "${OS}" = "Darwin" ]; then
  C_AGENT="src-agent-c/libdislagent.jnilib"
  RE_AGENT="src-re-agent/libdislreagent.jnilib"
else
  C_AGENT="src-agent-c/libdislagent.so"
  RE_AGENT="src-re-agent/libdislreagent.so"
fi

# ipc.socket is the default communication
# to enable shared memory, remove ",ipc.socket" from the options
java -agentpath:${C_AGENT} \
     -agentpath:${RE_AGENT} \
     -javaagent:build/dislagent-unspec.jar \
     -Xbootclasspath/a:build/dislagent-unspec.jar:build/dislinstr.jar:build/dislre-dispatch-unspec.jar \
     -cp bin/ \
      $*
