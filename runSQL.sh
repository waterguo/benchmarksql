#!/bin/bash

mvn -q exec:exec -Dexec.executable=java -Dexec.args="-cp %classpath -Dprop=$1 -DcommandFile=$2 ExecJDBC"
