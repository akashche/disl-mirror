#!/bin/bash

BASE_DIR="$( cd -P "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

OS=`uname`

if [ "${OS}" = "Darwin" ]; then
  C_AGENT="${BASE_DIR}/src-agent-c/libdislagent.jnilib"
  RE_AGENT="${BASE_DIR}/src-re-agent/libdislreagent.jnilib"
else
  C_AGENT="${BASE_DIR}/src-agent-c/libdislagent.so"
  RE_AGENT="${BASE_DIR}/src-re-agent/libdislreagent.so"
fi

# ipc.socket is the default communication
# to enable shared memory, remove ",ipc.socket" from the options
java -agentpath:${C_AGENT} \
     -agentpath:${RE_AGENT} \
     -javaagent:${BASE_DIR}/build/dislagent-unspec.jar \
     -Xbootclasspath/a:${BASE_DIR}/build/dislagent-unspec.jar:${BASE_DIR}/build/dislinstr.jar:${BASE_DIR}/build/dislre-dispatch-unspec.jar \
     -cp ${BASE_DIR}/bin/ \
      $*
