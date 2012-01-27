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


java -Ddislserver.port="1234" \
     -Ddebug=true \
     $* \
     -jar build/dislserver-unspec.jar \
     &

echo $! > ${SERVER_FILE}
