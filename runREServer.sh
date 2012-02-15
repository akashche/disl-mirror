#!/bin/sh

# available options
#    -Ddebug=true \
#    -Ddislserver.port="portNum" \

java $* \
     -jar build/dislre-server-unspec.jar \
     &

echo $! > re_${SERVER_FILE}
