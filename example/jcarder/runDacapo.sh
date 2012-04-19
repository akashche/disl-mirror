#!/bin/bash


iall=`java -jar /Users/aibeksarimbekov/Software/dacapo-9.12-bach.jar -l` 
all='
avrora
batik
eclipse
fop
h2
jython
luindex
lusearch
pmd
sunflow
tomcat
tradebeans
tradesoap
xalan
'


for bench in ${all}
do

./run.sh $bench 

done

