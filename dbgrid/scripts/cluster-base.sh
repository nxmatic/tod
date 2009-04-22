echo JobID: $JOB_ID - Task ID: $SGE_TASK_ID

source ./common

echo cluster-base - called with $*

if [ $SGE_TASK_ID -eq 1 ] 
then
	echo MASTER
	MASTER_HOST=`hostname`
	echo $MASTER_HOST > master-host
	echo Starting master, host: $MASTER_HOST
	./$1 "$2" "$3" "$4" "$5" "$6"
	rm master-host
else
	echo NODE
	
# 	echo lockfile -r 0 $LOCK_FILE
# 	lockfile -r 0 $LOCK_FILE
# 	RET=$?
# 	if [ $RET -ne 0 ] 
# 	then
# 		echo "Lock file found on $HOSTNAME, exiting."
# 		exit 1
# 	fi
	
	
	sleep 5s
	until [ -e master-host ] 
	do 
		echo Waiting...
		sleep 1s 
	done
	MASTER_HOST=`cat master-host`
	echo Starting node, connecting to $MASTER_HOST
	sleep 5s
	export TASK_ID=$SGE_TASK_ID
	./start-node.sh $MASTER_HOST $2
	
# 	rm -f $LOCK_FILE
fi


