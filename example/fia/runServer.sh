#!/bin/sh

OS=`uname`
ARCH_RAW=`uname -m`
ARCH="unspec"

if [ "${ARCH_RAW}" = "i686" -o "${ARCH_RAW}" = "x86_32"]; then
  ARCH="x32"
elif [ "${ARCH_RAW}" = "x86_64" ]; then
  ARCH="x64"
else
  echo "Unknow architecture.."
  exit -1
fi

if [ "${OS}" = "Darwin" ]; then
  JBORAT_AGENT_PATH="macosx/${ARCH}"
elif  [ "${OS}" = "Linux" ]; then
  JBORAT_AGENT_PATH="linux/${ARCH}"
else
  echo "Unknow platform..."
  exit -1
fi

CLASSPATH=../../lib/jborat-agent.jar:../../lib/jborat-runtime.jar:../../lib/jborat-interface.jar:lib/remote-server.jar

java -Dch.usi.dag.jborat.instrumented="instrumented" \
    -Djborat.debug \
    -Ddisl.dynbypass=true \
    -Ddisl.debug \
    -Djborat.ipc.socket=true \
    -Djava.library.path=./lib/${JBORAT_AGENT_PATH} \
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
