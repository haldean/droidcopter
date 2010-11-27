#!/bin/bash

for i in {0..255}; do
    echo -e "$i $i $i $i\r\n"
done

for i in {255..0}; do
    echo -e "$i $i $i $i\r\n"
done