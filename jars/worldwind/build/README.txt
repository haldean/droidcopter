$Id: README.txt 9208 2009-03-05 23:42:38Z jlittle $

####################################################
# NASA World Wind Java SDK - Build script overview #
####################################################

These are the Build scripts for NASA World Wind Java SDK.

## Intended Audience:
These scripts are intended to be used by developers wishing to update/modify 
the World Wind source files, and recompile or re-generate a fresh worldwind.jar

By default, the initial worldwind.jar file should be runnable simply by uzipping 
worldwind-0.6.xx.yyyy.zip into a local folder (to be referenced as WORLDWIND_HOME) 
and launching it with java:

	java -jar WORLDWIND_HOME/worldwind.jar
	
If you wish to make changes to the java source, it can be found in:

	WORLDWIND_HOME/src

In order to make a new jar, simply use the existing ANT target:

	ant worldwind.jarfile

This will recompile soucre as needed, and package into a new worldwind.jar file, 
and place it in WORLDWIND_HOME.
	
Note: this is also the default target for the ANT scripts, so simply 
entering "ant" from WORLDWIND_HOME will work as well

Run "ant -p" to see all available ANT targets

##############################
# Note on build dependencies #
##############################

In order for World WInd Java to build and run properly, jogl libraries 
MUST be present in the WORLDWIND_JAVA directory.  If they are not there, 
run the following ANT target to set up jogl for your local environment:
	
	ant jogl-setup
	

Build scripts updated by:
Justin Little
justin@justinlittle.com
03/2009