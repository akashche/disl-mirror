# /bin/sh

EXPECTED_ARGS=1

if [ $# -ne $EXPECTED_ARGS ]
then
    echo "Usage: `basename $0` {test case}"
    exit
fi

TARGET_CLASS="ch.usi.dag.disl.test.$1.TargetClass"

java -javaagent:../lib/jborat-agent.jar \
    -Dch.usi.dag.jborat.exclusionList="conf/exclusion.lst" \
    -Dch.usi.dag.jborat.liblist="conf/lib.lst" \
    -Dch.usi.dag.jborat.udiliblist="conf/udilib.lst" \
    -Dch.usi.dag.jborat.instrumentation="ch.usi.dag.disl.DiSL" \
    -Dch.usi.dag.jborat.codemergerList="conf/codemerger.lst" \
    -Dch.usi.dag.jborat.uninstrumented="uninstrumented" \
    -Dch.usi.dag.jborat.instrumented="instrumented" \
    -Xbootclasspath/p:lib/Thread_test.jar:lib/test-runtime.jar:../lib/jborat-runtime.jar:../lib/jborat-interface.jar \
    -Ddisl.dynbypass=true \
    -Ddisl.analysis.umidfile="dico.log" \
    -cp "./bin/"  ${TARGET_CLASS}
