#This is a simple example of a Sun Grid Engine batch script
#
#$ -cwd
#$ -j y
#$ -M gpothier@gmail.com
#$ -notify
#$ -P dbparallel
#$ -N netbench
#$ -S /bin/bash

./start-netbench.sh send syntagma
