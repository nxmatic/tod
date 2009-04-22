#! /bin/sh

echo PostgreSQL setup...
/usr/bin/psql -p 5433 <$1
echo Done.
