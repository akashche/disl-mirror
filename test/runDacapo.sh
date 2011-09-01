# /bin/sh


echo "RUNNING DACAPO BENCH $1"

java   -javaagent:../lib/jborat-agent.jar  -Dch.usi.dag.jborat.exclusionList="conf/exclusion.lst" -Dch.usi.dag.jborat.liblist="conf/lib.lst" -Dch.usi.dag.jborat.instrumentation="ch.usi.dag.disl.DiSL" -Dch.usi.dag.jborat.codemergerList="conf/codemerger.lst" -Dch.usi.dag.jborat.uninstrumented="uninstrumented" -Dch.usi.dag.jborat.instrumented="instrumented" -Xbootclasspath/p:lib/Thread_test.jar:lib/test-runtime.jar:../lib/jborat-runtime.jar -Ddisl.dynbypass=yes -Ddisl.analysis.umidfile="dico.log"  -cp "./bin/" -jar  dacapo-9.12-bach.jar --no-validation $1 $2 $3 
