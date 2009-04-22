#This is a simple example of a Sun Grid Engine batch script
#
#$ -cwd
#$ -j y
#$ -M gpothier@gmail.com
#$ -notify
#$ -P dbparallel
#$ -N dispatch
#$ -S /bin/sh

[ $SGE_TASK_ID -ne 1 ] && export EXTRA_JVM_ARGS="$EXTRA_JVM_ARGS -Dskip-events=true"
./cluster-base.sh "./start-replay.sh" $*
