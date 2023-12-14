#!/bin/bash

# abort on first error
set -e

# change directory
cd src/main/java/ghaffarian/progex/java/parser/

# clean existing files
rm *.java *.tokens

# defile vars
export CLASSPATH=".:/root/.m2/repository/org/antlr/antlr4/4.13.1/antlr4-4.13.1-complete.jar:$CLASSPATH"
antlr4='java -Xmx3G  org.antlr.v4.Tool'

# build parser
$antlr4 -visitor -package progex.java.parser Java.g4

# change back directory
cd ../../../../../../..

