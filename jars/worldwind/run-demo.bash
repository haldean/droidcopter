#!/bin/bash
# Run a WorldWind Demo
# $Id: run-demo.bash 3950 2007-12-14 23:27:08Z tgaskins $

echo Running $1
java -Xmx512m -Dsun.java2d.noddraw=true -classpath ./src:./classes:./worldwind.jar:./jogl.jar:./gluegen-rt.jar $*
