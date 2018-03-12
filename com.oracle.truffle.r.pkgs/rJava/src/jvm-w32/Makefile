# helper tools and libs for building and running rJava for Windows
# Author: Simon Urbanek
#         with contributions from Brian Ripley

include $(R_HOME)/etc$(R_ARCH)/Makeconf

TARGETS=libjvm.dll.a findjava.exe

# libjvm.dll.a    - wrapper lib for jvm.dll from Java
# findjava.exe    - helper tool to find the current JDK from the registry

all: $(TARGETS)

# detect 64-bit Windows
ifeq ($(strip $(shell $(R_HOME)/bin/R --slave -e 'cat(.Machine$$sizeof.pointer)')),8)
JVM64DEF=64
endif

libjvm.dll.a: jvm$(JVM64DEF).def
	$(DLLTOOL) --input-def $^ --kill-at --dllname jvm.dll --output-lib $@

# compile findjava.exe from source - no magic here, no special libs necessary
findjava.o: findjava.c
	$(CC) -O2 -c -o $@ $^

findjava.exe: findjava.o
	$(CC) -s -o $@ $^

# just cleanup everything
clean:
	rm -f *.o *~ $(TARGETS)

.PHONY: all clean
