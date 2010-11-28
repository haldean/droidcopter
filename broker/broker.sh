#!/bin/bash

ncat --broker --listen 7000 -o ~/logs/`date +"%y%m%d-%H%M%S"`.txt &
ncat --broker --listen 7001 &