#! /bin/bash

for n in $*
do
  base=`strip-suffix $n`
  
  echo $n | egrep '(.*-s\.)|(.*~$)' - >/dev/null
  if [ "$?" != "0" ]
  then
    echo "Converting $n"
    convert $n -scale 400 -quality 50 $base-s.jpg
    convert $n -scale 400 $base-s.png
  fi
done