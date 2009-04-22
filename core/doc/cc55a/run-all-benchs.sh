#! /bin/sh

HOME=/home/gpothier/eclipse/workbench-3.1/TOD

ORACLE_CP=/usr/lib/oracle/xe/app/oracle/product/10.2.0/server/jdbc/lib/ojdbc14.jar
POSTGRES_CP=/home/gpothier/installs/java/postgresql-8.1-405.jdbc3.jar
BERKELEY_CP=/home/gpothier/apps/java/je-2.1.30/lib/je.jar

BASE_CP=$HOME/bin:/home/gpothier/eclipse/workbench-3.1/zz.utils/bin

MAIN=tod.experiments.bench.InsertBench
RAW=tod.experiments.bench.RawStorageCollector
ORACLE=tod.experiments.bench.OracleCollector
POSTGRES=tod.experiments.bench.PostgresCollector
POSTGRES_LIGHT=tod.experiments.bench.PostgresCollectorLight
BERKELEY=tod.experiments.bench.BerkeleyCollector

cd $HOME

rm Bench-*.txt

java -cp $BASE_CP:$BERKELEY_CP $MAIN $BERKELEY 100000 > Bench-Berkeley-100000.txt
java -cp $BASE_CP:$BERKELEY_CP $MAIN $BERKELEY 1000000 > Bench-Berkeley-1000000.txt
java -cp $BASE_CP:$BERKELEY_CP $MAIN $BERKELEY 10000000 > Bench-Berkeley-10000000.txt

#java -cp $BASE_CP $MAIN $RAW 1000000 > Bench-Raw-1000000.txt
#java -cp $BASE_CP $MAIN $RAW 10000000 > Bench-Raw-10000000.txt
#java -cp $BASE_CP $MAIN $RAW 100000000 > Bench-Raw-100000000.txt

#java -cp $BASE_CP:$ORACLE_CP $MAIN $ORACLE 100 > Bench-Oracle-100.txt
#java -cp $BASE_CP:$ORACLE_CP $MAIN $ORACLE 1000 > Bench-Oracle-1000.txt
#java -cp $BASE_CP:$ORACLE_CP $MAIN $ORACLE 10000 > Bench-Oracle-10000.txt
#java -cp $BASE_CP:$ORACLE_CP $MAIN $ORACLE 100000 > Bench-Oracle-100000.txt
#java -cp $BASE_CP:$ORACLE_CP $MAIN $ORACLE 1000000 > Bench-Oracle-1000000.txt

#java -cp $BASE_CP:$POSTGRES_CP $MAIN $POSTGRES 100 > Bench-Postgres-100.txt
#java -cp $BASE_CP:$POSTGRES_CP $MAIN $POSTGRES 1000 > Bench-Postgres-1000.txt
#java -cp $BASE_CP:$POSTGRES_CP $MAIN $POSTGRES 10000 > Bench-Postgres-10000.txt
#java -cp $BASE_CP:$POSTGRES_CP $MAIN $POSTGRES 100000 > Bench-Postgres-100000.txt

#java -cp $BASE_CP:$POSTGRES_CP $MAIN $POSTGRES_LIGHT 100 > Bench-Postgres-Light-100.txt
#java -cp $BASE_CP:$POSTGRES_CP $MAIN $POSTGRES_LIGHT 1000 > Bench-Postgres-Light-1000.txt
#java -cp $BASE_CP:$POSTGRES_CP $MAIN $POSTGRES_LIGHT 10000 > Bench-Postgres-Light-10000.txt
#java -cp $BASE_CP:$POSTGRES_CP $MAIN $POSTGRES_LIGHT 100000 > Bench-Postgres-Light-100000.txt
