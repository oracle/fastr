JAVA=java
JAVAC=javac
JFLAGS+=-encoding utf8 -source 1.6 -target 1.6

all: test PlotDemo.class StartRserve.class

../Rserve.jar: ../../REngine.jar
	$(MAKE) -C .. Rserve.jar

../../REngine.jar:
	$(MAKE) -C ../.. REngine.jar

test: test.class ../Rserve.jar ../../REngine.jar
	$(JAVA) -cp ../Rserve.jar:../../REngine.jar:. test

test.class: test.java ../Rserve.jar ../../REngine.jar
	$(JAVAC) $(JFLAGS) -d . -cp ../Rserve.jar:../../REngine.jar:. test.java

PlotDemo.class: PlotDemo.java ../Rserve.jar ../../REngine.jar
	$(JAVAC) $(JFLAGS) -d . -cp ../Rserve.jar:../../REngine.jar:. $<

PlotDemo: PlotDemo.class ../Rserve.jar ../../REngine.jar
	$(JAVA) -cp ../Rserve.jar:../../REngine.jar:. $@

jt: jt.class ../Rserve.jar ../../REngine.jar
	$(JAVA) -cp ../Rserve.jar:../../REngine.jar:. $@

jt.class: jt.java ../Rserve.jar ../../REngine.jar
	$(JAVAC) $(JFLAGS) -d . -cp ../Rserve.jar:../../REngine.jar:. jt.java

StartRserve.class: StartRserve.java ../Rserve.jar ../../REngine.jar
	$(JAVAC) $(JFLAGS) -d . -cp ../Rserve.jar:../../REngine.jar:. StartRserve.java

clean:
	rm -rf org *~ *.class

.PHONY: test all clean

