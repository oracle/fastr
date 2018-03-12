## there are two ways to compile JRIEngine - either use
## already installed REngine and JRI and then just
## compile JRIEngine against those jars or use this
## Makefile with all org sources checked out
## in which case the JRIEngine.jar will implicitly
## contain org.rosuda.JRI classes as well
## However, that requires both:
## org/rosuda/JRI
## org/rosuda/REngine

JRI_MAIN_SRC=$(wildcard ../../*.java)
JRIENGINE_SRC=JRIEngine.java

JAVAC=javac
JAR=jar
JFLAGS=-source 1.6 -target 1.6

all: JRIEngine.jar

JRIEngine.jar: $(JRI_MAIN_SRC) $(JRIENGINE_SRC)
	rm -rf org
	$(JAVAC) $(JFLAGS) -classpath ../REngine.jar -d . $^
	$(JAR) fc $@ org

clean:
	rm -rf org *~ JRIEngine.jar *.class
	make -C test clean
