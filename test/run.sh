# /bin/sh

EXPECTED_ARGS=1

if [ $# -ne $EXPECTED_ARGS ]
then
    echo "Usage: `basename $0` {test case}"
    exit
fi

DISL_CLASS="./bin/ch/usi/dag/disl/test/$1/DiSLClass.class"
TARGET_CLASS="ch.usi.dag.disl.test.$1.TargetClass"

java -javaagent:../lib/jborat-agent.jar -Dch.usi.dag.jborat.exclusionList="conf/exclusion.lst" -Dch.usi.dag.jborat.liblist="conf/lib.lst" -Dch.usi.dag.jborat.instrumentation="ch.usi.dag.disl.DiSL" -Dch.usi.dag.jborat.codemergerList="conf/codemerger.lst" -Dch.usi.dag.jborat.uninstrumented="uninstrumented" -Dch.usi.dag.jborat.instrumented="instrumented" -Xbootclasspath/p:../lib/Thread_jborat.jar:../lib/jborat-runtime.jar -Ddisl.classes=${DISL_CLASS} -Ddisl.dynbypass -Ddisl.debug -cp "./bin/" ${TARGET_CLASS}
