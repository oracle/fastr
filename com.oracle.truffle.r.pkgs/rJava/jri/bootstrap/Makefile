# Makefile for JRI bootstrap
# Win/OSX should be ok, Linux will need some tweaking

all: boot.jar

OSKIND=$(shell if echo "${OS}"|grep -i windows >/dev/null 2>&1; then echo win32; else uname -s; fi)

ifeq ($(OSKIND),win32)
ifeq ($(JAVA_HOME),)
JAVA_HOME=N:/java/jdk1.5.0
endif
JSO_PREFIX=
JSO_SUFFIX=.dll
JCPPFLAGS=-DWIN32 -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/win32
JLDFLAGS=-shared -Wl,--add-stdcall-alias -mno-cygwin
JLIBS=-L$(JAVA_HOME)/jre/bin/client -L$(JAVA_HOME)/jre/bin -ljvm
PATHSEP=\;
KNOWNOS=yes
OSNAME=Windows
JAVA=$(JAVA_HOME)/bin/java
JAVAC=$(JAVA_HOME)/bin/javac
JAR=$(JAVA_HOME)/bin/jar
endif
ifeq ($(OSKIND),Darwin)
JSO_PREFIX=lib
JSO_SUFFIX=.jnilib
JLDFLAGS=-dynamiclib
JLIBS=-framework JavaVM
JCPPFLAGS=-I/System/Library/Frameworks/JavaVM.framework/Headers
PATHSEP=:
KNOWNOS=yes
OSNAME=MacOSX
JAVA=java
JAVAC=javac
JAR=jar
endif
ifneq ($(KNOWNOS),yes)
ifeq ($(JAVA_HOME),)
JAVA_HOME=/usr/lib/java
endif
JSO_PREFIX=lib
JSO_SUFFIX=.so
JCPPFLAGS=-I$(JAVA_HOME)/include -I$(shell dirname `find $(JAVA_HOME)/include -name jni_md.h|sed -n -e 1p`)
JLDFLAGS=-shared
JLIBS=-L$(JAVA_HOME)/lib -ljvm
PATHSEP=:
OSNAME="generic unix"
JAVA=java
JAVAC=javac
JAR=jar
endif

$(JSO_PREFIX)boot$(JSO_SUFFIX): JRIBootstrap.o
	$(CC) $(JLDFLAGS) -o $@ $^ #$(JLIBS)

JRIBootstrap.o: JRIBootstrap.c JRIBootstrap.h
	$(CC) -c -o $@ $< $(JCPPFLAGS)

run: boot.jar
	$(JAVA) -jar $<

clean:
	rm -f JRIBootstrap.o $(JSO_PREFIX)boot$(JSO_SUFFIX) *.class *~

boot.jar: $(JSO_PREFIX)boot$(JSO_SUFFIX)
	$(JAVAC) -source 1.2 -target 1.2 *.java
	$(JAR) fcm $@ mft *.class $^

.PHONY: clean run all
