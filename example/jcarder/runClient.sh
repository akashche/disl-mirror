#!/bin/bash

OS=`uname`
 
if [ "${OS}" = "Darwin" ]; then
  C_AGENT="../../src-agent-c/libdislagent.jnilib"
else
  C_AGENT="../../src-agent-c/libdislagent.so"
fi

all=' apparat actors'
iall=`java -jar /Users/aibeksarimbekov/Software/scala-benchmark.jar -l` 
iiall=' apparat actors  batik dummy  factorie  h2 jython kiama  scalac scaladoc scalap scalariform scalatest scalaxb specs sunflow tmt  xalan '

for bench in ${all}
do
echo Starting $bench
java -Xmx2g -XX:MaxPermSize=128m  -noverify -agentpath:${C_AGENT}   -javaagent:../../build/dislagent-unspec.jar  \
    -Xbootclasspath/a:../../build/dislagent-unspec.jar:../../build/dislinstr.jar \
    -cp bin/ \
    -jar /Users/aibeksarimbekov/Software/scala-benchmark.jar --preserve $bench     
# $*
echo Finished $bench
done
