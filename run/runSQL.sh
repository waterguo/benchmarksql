myCP="../lib/postgresql-9.2-1002.jdbc4.jar"
myCP="$myCP:../lib/log4j-1.2.17.jar"
myCP="$myCP:../dist/BenchmarkSQL-3.0.jar"

myOPTS="-Dprop=$1"
myOPTS="$myOPTS -DcommandFile=$2"

java -cp $myCP $myOPTS ExecJDBC 
