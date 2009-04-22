#This is a simple example of a Sun Grid Engine batch script
#
#$ -cwd
#$ -j y
#$ -M gpothier@gmail.com
#$ -notify
#$ -P dbparallel
#$ -N lock
#$ -S /bin/sh

sleep 10000