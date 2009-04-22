#! /bin/bash

source common
export RAW_EVENTS_DIR="$1"
./launch.sh replay "$2" "$3" "$4" "$5" "$6"
