#! /bin/sh

# This script pushes the library to a server

LOC=$1
NAME=$2

echo `./svn-sig.sh` >$NAME-sig.txt
scp $LOC $NAME-sig.txt pleiad@pleiad.cl:/home/v/pleiad/www/files/tod/tod-agent/trunk