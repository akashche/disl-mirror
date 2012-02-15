#!/bin/bash

OS=`uname`

if [ "${OS}" = "Darwin" ]; then
  C_AGENT="../../src-agent-c/libdislagent.jnilib"
else
  C_AGENT="../../src-agent-c/libdislagent.so"
fi

# ipc.socket is the default communication
# to enable shared memory, remove ",ipc.socket" from the options
java -Xmx2g -XX:MaxPermSize=128m -noverify -agentpath:${C_AGENT} \
     -javaagent:../../build/dislagent-unspec.jar \
     -Xbootclasspath/a:../../build/dislagent-unspec.jar:../../build/dislinstr.jar \
     -cp bin/ \
-jar /Users/aibeksarimbekov/Software/dacapo-9.12-bach.jar batik  
#      $*
