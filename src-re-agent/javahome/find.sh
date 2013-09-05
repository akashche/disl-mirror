#!/bin/bash

if [ -f "var.local" ]; then
	cp -f "var.local" "var"
	exit 0	
fi

if [ ! -z "$JAVA_HOME" ]; then
	echo "JAVA_HOME="$JAVA_HOME > "var"
	exit 0
fi

JAVAC=`which javac 2>/dev/null`

JH=`readlink -f ${JAVAC}`

JH=`echo ${JH%/*}`
JH=`echo ${JH%/*}`

echo "JAVA_HOME="${JH} > "var"
