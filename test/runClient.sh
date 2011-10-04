#!/bin/bash

java -XX:MaxPermSize=128m \
    -Xmx2G \
    -javaagent:lib/agent.jar \
    -Xbootclasspath/p:lib/remote-runtime.jar:../lib/jborat-runtime.jar:build/test-runtime.jar \
    -agentpath:lib/libjboratclientagent.so=1234,localhost \
    -cp ./bin \
     $*
