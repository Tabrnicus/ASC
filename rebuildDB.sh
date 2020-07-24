#!/usr/bin/env bash

# Store directory to the current one the script resides in
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

# Store path for ASC.sqlite3
DBFILE="${DIR}/build/libs/resources/ASC.sqlite3"

# Store path to sqlite commands
SQLITECOMMANDS="${DIR}/createdb.sql"

# Remove the old DB
rm -f "${DBFILE}"

# Set up new DB
sqlite3 -batch "${DBFILE}" < "${SQLITECOMMANDS}"

echo "Done."
