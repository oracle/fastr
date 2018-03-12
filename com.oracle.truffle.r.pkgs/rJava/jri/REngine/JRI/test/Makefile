## NOTE: the tests require rJava to be installed in the current R
##       since they also test the JRI/rJava connectivity

CP=../../REngine.jar:../JRIEngine.jar:$(RJAVA)/java/boot:$(RJAVA)/jri/JRI.jar
JFLAGS=-source 1.6 -target 1.6

RJAVA=$(shell echo "cat(system.file(package='rJava'))"|R --slave --no-save)
JRI=$(shell if test -e ../../../JRI/src/JRI.jar; then echo ../../../JRI/src; else echo $(RJAVA)/jri; fi)

JRT=-Djava.library.path=$(JRI):.

JAVAC=javac
JAVA=java

all: run

RTest.class: RTest.java
	$(JAVAC) $(JFLAGS) -classpath $(CP) $^

run: RTest.class
	R CMD $(JAVA) -cp $(CP):. $(JRT) RTest

clean:
	rm -rf org *.class *~

.PHONY: run clean

