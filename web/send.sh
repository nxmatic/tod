#! /bin/sh

rsync -avz --partial --del -e "ssh" --rsync-path "/usr/bin/rsync --server --daemon --config=/home/v/pleiad/.rsyncd.conf ." ./output/ pleiad@pleiad.dcc.uchile.cl::tod-web
