#!/bin/bash

if [ "$1" == '' ]; then
    LOGFILE=~/logs/`date +"%y%m%d-%H%M%S"`.txt
else
    LOGFILE=$1
fi

echo Logging to $LOGFILE
ncat --broker --listen 7000 -o $LOGFILE &
ncat --broker --listen 7001 &