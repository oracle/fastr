RENG_SRC=$(wildcard ../*.java)
RSRV_SRC=$(wildcard *.java) $(wildcard protocol/*.java)

TARGETS=Rserve.jar

all: $(TARGETS)

JAVAC=javac
JFLAGS=-encoding utf8 -source 1.6 -target 1.6

../REngine.jar: $(RENG_SRC)
	make -C .. REngine.jar

Rserve.jar: $(RSRV_SRC) ../REngine.jar
	@rm -rf org
	$(JAVAC) -d . -cp ../REngine.jar $(RSRV_SRC)
	jar fc $@ org
	rm -rf org

clean:
	rm -rf org *~ protocol/*~ $(TARGETS)
	make -C test clean

test:
	make -C test test

.PHONY: clean all test
