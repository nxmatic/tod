#! /bin/bash

case "$1" in
	"session"	) MAIN="tod.impl.dbgrid.GridSession";;
	"master"	) MAIN="tod.impl.dbgrid.GridMaster";;
	"node"		) MAIN="tod.impl.dbgrid.StartNode";;
	"store"		) MAIN="tod.utils.StoreTODServer";;
	"replay"	) MAIN="tod.impl.dbgrid.Replayer";;
	"query"		) MAIN="tod.impl.dbgrid.bench.GridQuery";;
	"nodestore"	) MAIN="tod.impl.dbgrid.bench.BenchDatabaseNode";;
	"dispatch"	) MAIN="tod.impl.dbgrid.bench.GridDispatch";;
	"netbench"	) MAIN="tod.impl.dbgrid.bench.NetBench";;
	*		) MAIN=$1;;
esac

VMARGS=''
VMARGS="$VMARGS -Xmx$JVM_HEAP_SIZE"
VMARGS="$VMARGS -XX:MaxDirectMemorySize=$JVM_DIRECTMEM_SIZE"
VMARGS="$VMARGS -Djava.library.path=$NATIVE"
VMARGS="$VMARGS -ea"
VMARGS="$VMARGS -server"
VMARGS="$VMARGS -cp $CLASSPATH"
VMARGS="$VMARGS -Dnode-data-dir=$DATA_DIR"
VMARGS="$VMARGS -Dmaster-host=$MASTER_HOST"
VMARGS="$VMARGS -Devents-file=$EVENTS_FILE"
VMARGS="$VMARGS -Dlocations-file=$LOCATIONS_FILE"
VMARGS="$VMARGS -Dpage-buffer-size=$PAGE_BUFFER_SIZE"
VMARGS="$VMARGS -Dtask-id=$TASK_ID"
VMARGS="$VMARGS -Dgrid-impl=$GRID_IMPL"
VMARGS="$VMARGS -Dcheck-same-host=$CHECK_SAME_HOST"
VMARGS="$VMARGS -Ddb-raw-events-dir=$RAW_EVENTS_DIR"

if [ -n "$JDWP_PORT" ]
then
	VMARGS="$VMARGS -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=$JDWP_PORT"
fi

echo Host: `hostname`
#$JAVA_HOME/bin/java -version 
$JAVA_HOME/bin/java $VMARGS $EXTRA_JVM_ARGS $MAIN $2 $3 $4 $5
