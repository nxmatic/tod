#This is a simple example of a Sun Grid Engine batch script
#
#$ -cwd
#$ -j y
#$ -M gpothier@gmail.com
#$ -notify
#$ -P dbparallel
#$ -N nodes
#$ -S /bin/sh

./cluster-base.sh "./start-master.sh" $1
