#!/bin/bash

mvn -q exec:exec -Dexec.executable=java -Dexec.args="-cp %classpath -Dprop=$1 LoadData $2 $3 $4 $5"
