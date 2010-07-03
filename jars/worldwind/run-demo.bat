REM Windows Batch file for Running a WorldWind Demo
REM $Id: run-demo.bat 13145 2010-02-17 03:55:11Z dcollins $

@echo Running %1
java -Xmx512m -Dsun.java2d.noddraw=true -classpath .\src;.\classes;.\worldwind.jar;.\jogl.jar;.\gluegen-rt.jar %*
