#!/bin/bash

if [ '' == "$1" ]; then
    echo 'Usage: speed-test.sh [block device of Arduino]'
    exit
fi

if [ ! -e "parse-random" ]; then
    echo 'Compiling parse-random utility.'
    gcc parse-random.c -o parse-random
fi

echo 'Testing transfer speed using random ASCII data from urandom.'
/usr/bin/time -f %e -o timing.dat cat /dev/urandom | ./parse-random > /dev/ttyUSB2
echo 'Motor commands per second:'
echo 10 k 1000 `tail -1 timing.dat` / p | dc