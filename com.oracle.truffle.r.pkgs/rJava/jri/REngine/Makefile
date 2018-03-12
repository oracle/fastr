RENG_SRC=$(wildcard *.java)
RSRV_SRC=$(wildcard Rserve/*.java) $(wildcard Rserve/protocol/*.java)
JRI_SRC=$(wildcard JRI/*.java)

TARGETS=REngine.jar Rserve.jar

all: $(TARGETS)

JAVAC=javac
JAVADOC=javadoc
JDFLAGS=-author -version -breakiterator -link http://java.sun.com/j2se/1.4.2/docs/api/
JFLAGS+=-source 1.6 -target 1.6

REngine.jar: $(RENG_SRC)
	@rm -rf org
	$(JAVAC) -d . $(JFLAGS) $(RENG_SRC)
	jar fc $@ org
	rm -rf org

Rserve.jar: $(RSRV_SRC) REngine.jar
	@rm -rf org
	$(JAVAC) -d . -cp REngine.jar $(RSRV_SRC)
	jar fc $@ org
	rm -rf org

clean:
	rm -rf org *~ $(TARGETS) doc
	make -C Rserve clean
	make -C JRI clean

test:
	make -C Rserve test

rc:	Rserve.jar Rserve/test/jt.java
	make -C Rserve/test jt

doc:	$(RENG_SRC) $(RSRV_SRC) $(JRI_SRC)
	rm -rf $@
	mkdir $@
	$(JAVADOC) -d $@ $(JDFLAGS) $^

mvn.pkg:
	mvn clean package install && (cd Rserve && mvn clean package)

mvn.sign:
	mvn clean verify install -P release && (cd Rserve && mvn clean verify -P release )

mvn.deploy:
	mvn clean deploy install -P release && (cd Rserve && mvn clean deploy -P release )

.PHONY: clean all test
