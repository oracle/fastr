JAVA_SRC=$(wildcard *.java)
JFLAGS=-source 1.2 -target 1.2
JAVAC=javac
JAVA=java
JAVADOC=javadoc
JAVADOCFLAGS=-author -version -breakiterator -link http://java.sun.com/j2se/1.4.2/docs/api

all: compile test

compile: $(JAVA_SRC)
	$(JAVAC) $(JFLAGS) $(JAVA_SRC)

test_RJavaTools: compile
	$(JAVA) RJavaTools_Test

test_RJavaArrayTools: compile
	$(JAVA) RJavaArrayTools_Test

test_ArrayWrapper:
	$(JAVA) ArrayWrapper_Test

test_RectangularArrayBuilder:
	$(JAVA) RectangularArrayBuilder_Test

test: compile test_RJavaTools test_RJavaArrayTools test_ArrayWrapper test_RectangularArrayBuilder

javadoc:
	$(JAVADOC) $(JAVADOCFLAGS) -d javadoc $(JAVA_SRC)

clean:
	rm -rfv *.class *~

.PHONY: all clean

