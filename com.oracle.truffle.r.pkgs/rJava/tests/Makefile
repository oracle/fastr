RBIN=R
RARGS=--vanilla --slave
JAVAC=javac
JFLAGS=-source 1.2 -target 1.2

TESTS=types.R leaks.R
SOURCES=Types.java Leaks.java

TEST_RES=$(TESTS:%.R=%.test)
CLASSES=$(SOURCES:%.java=%.class)

retest: clean-tests test

test: $(CLASSES) $(TEST_RES)

.R.test:
	$(RBIN) $(RARGS) < $^ > $@

.java.class:
	$(JAVAC) $(JFLAGS) $^

clean: clean-tests
	rm -f *~ $(CLASSES)

clean-tests:
	rm -f $(TEST_RES)

.PHONY: clean test clean-tests retest

.SUFFIXES: .R .test .java .class
