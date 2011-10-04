#!/bin/sh

CLASSPATH=../lib/jborat-agent.jar:../lib/jborat-runtime.jar:../lib/jborat-interface.jar:lib/remote-server.jar

java -Dch.usi.dag.jborat.instrumented="instrumented" \
    -Djborat.debug \
    -Ddisl.dynbypass=true \
    -Ddisl.debug \
    -Dch.usi.dag.jborat.instrumentation="ch.usi.dag.disl.DiSL" \
    -Dch.usi.dag.jborat.codemergerList="conf/codemerger.lst" \
    -Dch.usi.dag.jborat.liblist="conf/lib.lst" \
    -Dch.usi.dag.jborat.udiliblist="conf/udilib.lst" \
    -Dch.usi.dag.jborat.exclusionList="conf/exclusion.lst" \
    $* \
    -cp ${CLASSPATH} \
    ch.usi.dag.jborat.remote.server.Server \
    &

echo $! > ${SERVER_FILE}