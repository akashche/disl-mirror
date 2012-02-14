#!/bin/sh

# available options
#    -Ddebug=true \
#    -Ddislserver.port="portNum" \

java -Ddebug=true \
     $* \
     -jar build/dislreserver-unspec.jar \
     &

echo $! > re_${SERVER_FILE}
