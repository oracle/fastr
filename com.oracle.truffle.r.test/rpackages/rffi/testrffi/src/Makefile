.PHONY: all clean

C_SOURCES := $(wildcard *.c)
# R uses the .so extension on Mac OS X
C_OBJECTS := $(C_SOURCES:.c=.o)
C_LIB := testrffi.so
CC = clang

$(warning cwd=$(CURDIR))
$(warning FASTR_HOME=$(FASTR_HOME))
$(warning FASTR_JAVA_HOME=$(FASTR_JAVA_HOME))

ifeq ($(FASTR_INSTALL), GNUR)
    INCLUDES := -I $(R_HOME)/include
    LDFLAGS :=  -dynamiclib -L/usr/local/lib -F${R_HOME}/../.. -framework R
else
    NATIVE_PROJECT=$(FASTR_HOME)/com.oracle.truffle.r.native
    include $(NATIVE_PROJECT)/platform.mk
    INCLUDES := -I $(NATIVE_PROJECT)/include/jni -I $(FASTR_JAVA_HOME)/include -I $(FASTR_JAVA_HOME)/include/$(OS_DIR)
endif


all: $(C_LIB)

$(C_LIB): $(C_OBJECTS)
	$(CC) $(LDFLAGS)  -o $(C_LIB) $(C_OBJECTS)

%.o: %.c
	$(CC) $(CFLAGS) $(INCLUDES) -c $< -o $@

clean:
	rm $(C_LIB) $(C_OBJECTS)

