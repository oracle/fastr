# helper tools and libs for building and running rJava for Windows
# Author: Simon Urbanek

include $(R_HOME)/etc$(R_ARCH)/Makeconf

TARGETS=libjvm.dll.a findjava.exe

# libjvm.dll.a    - wrapper lib for jvm.dll from Java
# findjava.exe    - helper tool to find the current JDK from the registry

all: $(TARGETS)

ifeq ($(strip $(shell $(R_HOME)/bin/R --slave -e 'cat(.Machine$$sizeof.pointer)')),8)
JVMDEF=jvm64.def
else
JVMDEF=jvm.def
endif

libjvm.dll.a: $(JVMDEF)
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
