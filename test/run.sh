# /bin/sh

java -javaagent:../lib/jborat-agent.jar -Dch.usi.dag.jborat.liblist="conf/lib.lst" -Dch.usi.dag.jborat.instrumentation="ch.usi.dag.disl.DiSL" -Dch.usi.dag.jborat.codemergerList="conf/codemerger.lst" -Dch.usi.dag.jborat.uninstrumented="uninstrumented" -Dch.usi.dag.jborat.instrumented="instrumented" -Xbootclasspath/p:../lib/Thread_jborat.jar:../lib/jborat-runtime.jar -Ddisl.classes="./bin/AnnotatedClass.class" -cp "./bin/" TargetClass
