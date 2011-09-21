#!/bin/sh

TESTS="after args bbmarker bodymarker bytecodemarker dynamicinfo exception exceptionhandler guard loop processor scope staticinfo syntheticlocal threadlocal tryclause"

for TEST in ${TESTS}
do
	echo "*** Starting test ${TEST} ***"
	./run.sh ${TEST} pkg 2>&1 | tee -a run-all.log
done
