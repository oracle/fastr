SRC:=$(wildcard *.java)
OBJ:=$(SRC:%.java=org/rosuda/javaGD/%.class)
JAR=javaGD.jar
JAPIURL:=http://java.sun.com/j2se/1.4.2/docs/api

TARGETS=$(JAR)

JAVAC=javac $(JFLAGS)

all: $(JAR)

#org/rosuda/javaGD/JavaGD.class: JavaGD.java org/rosuda/javaGD/GDCanvas.class
#	$(JAVAC) -d . $<

#org/rosuda/javaGD/%.class: %.java
#	$(JAVAC) -d . $^

doc: $(SRC)
	rm -rf JavaDoc; mkdir JavaDoc
	javadoc -d JavaDoc -author -version -breakiterator -link $(JAPIURL) $^

$(JAR): $(SRC)
	$(JAVAC) -d . $^
	jar fc $@ org

clean:
	rm -rf org *~ $(TARGETS)

.PHONY: all clean
