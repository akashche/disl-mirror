#!/bin/sh

# available options
#    -Ddebug=true \
#    -Ddisl.classes="list of disl classes (: - separator)"
#    -Ddisl.noexcepthandler=true \
#    -Ddisl.exclusionList="path" \
#    -Ddislserver.instrumented="path" \
#    -Ddislserver.uninstrumented="path" \
#    -Ddislserver.port="portNum" \
#    -Ddislserver.timestat=true \
#    -Ddislserver.continuous=true \


java $* \
    -Ddislserver.instrumented="instrumented" \
    -Ddebug=false \
    -Ddislserver.uninstrumented="uninstrumented" \
    -Ddislserver.continuous=true \
    -Ddisl.exclusionList="conf/exclusion.lst" \
    -jar ../../build/dislserver-unspec.jar \
     &

echo $! > ${SERVER_FILE}