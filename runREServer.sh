#!/bin/bash

BASE_DIR="$( cd -P "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# available options
#    -Ddebug=true \
#    -Ddislreserver.port="portNum" \

java $* \
     -jar ${BASE_DIR}/build/dislre-server-unspec.jar \
     &

echo $! > ${RE_SERVER_FILE}
