#!/bin/sh

TESTS=`ls src/ch/usi/dag/disl/test/`

for TEST in ${TESTS}
do
	echo "*** Starting test ${TEST} ***"
	./run.sh ${TEST} pkg 2>&1 | tee -a run-all.log
done
