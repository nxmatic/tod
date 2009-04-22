#! /bin/bash

source common

SCRIPT=$1
NODES=$2
SEC_NODES=$((NODES*$SUB_FACTOR))

echo NODES: $NODES SF: $SF SN: $SEC_NODES

# echo Removing lock files...
# cluster-fork rm -f $LOCK_FILE
# echo Lock files removed.

if [ -n "$SYNTAGMASTER" ] 
then
	echo "$SYNTAGMASTER" > master-host
	qsub -t 2-$((SEC_NODES+1)):1 ./cluster-$SCRIPT.sh $NODES $3 $4 $5
	./start-$SCRIPT.sh $NODES $3 $4 $5
else
	qsub -t 1-$((SEC_NODES+1)):1 ./cluster-$SCRIPT.sh $NODES $3 $4 $5
fi

