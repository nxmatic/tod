#! /bin/sh

deploy()
{
	HOST=$1
	DIR=$2
	
	rsync -avz -e ssh\
	 --rsync-path '/usr/bin/rsync \
	 --server --daemon --config=$HOME/rsync.conf .'\
	 --exclude ".svn/"\
	 $DIR\
	 gpothier@$HOST::tod
}

#deploy syntagma.dim.uchile.cl "$*" || exit 10
deploy dichato "$*" || exit 11

exit 0