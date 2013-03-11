#!/bin/sh
TEST_PATH=src-test/ch/usi/dag/disl/test

ls -1 $TEST_PATH | sort | while read TEST; do
	echo "*** Starting test ${TEST} ***" | tee -a run-all.log
	./runTest.sh ${TEST} pkg |& tee -a run-all.log
done
