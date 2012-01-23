#!/bin/sh

# available options
#    -Ddebug=true \
#    -Ddisl.noexcepthandler=true \
#    -Ddislserver.instrumented="path" \
#    -Ddislserver.uninstrumented="path" \
#    -Ddislserver.port="portNum" \
#    -Ddislserver.exclusionList="path"

java -Ddislserver.port="1234" \
     $* \
     -jar build/dislserver-unspec.jar \
     &

echo $! > ${SERVER_FILE}
