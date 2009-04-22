#! /bin/sh
# Compute a signature of the svn revision of the .cpp and .h files
FILES="`ls *.cpp *.h`"

cat $FILES | md5sum  | awk '{print $1}'
