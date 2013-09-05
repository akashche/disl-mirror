#!/bin/sh

TESTS=`ls src-test/ch/usi/dag/disl/test/`

for TEST in ${TESTS}
do
	echo "*** Starting test ${TEST} ***" | tee -a run-all.log
	./runTest.sh ${TEST} pkg 2>&1 | tee -a run-all.log
done
