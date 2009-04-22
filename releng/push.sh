#! /bin/sh

# This script pushes the release to a server

echo Copying files...
scp release/* pleiad@pleiad.dcc.uchile.cl:/home/v/pleiad/www/files/tod/releases

echo Committing revision info...
VERSION=`ls release/changes_*.txt |awk '{print substr($0, 17, length($0)-20)}'`
./releng.py -c $VERSION
