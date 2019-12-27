#
# Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 3 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 3 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 3 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#

.PHONY: all clean

$(info F2C_BINARY: $(F2C_BINARY))
	
OS_NAME := $(shell uname -s)

all: 

ifdef F2C_BINARY

ifeq ($(wildcard $(F2C_BINARY)),)
	$(error F2C_BINARY directory $(F2C_BINARY) does not exist!)	
else 
	$(info copying f2c binaries from $(F2C_BINARY))
	rm -fr ../../f2c
	cp -r $(F2C_BINARY)/f2c ../../f2c
	mkdir -p ../../bin
	cp ../../f2c/f2c/f2c ../../bin
endif # ifeq ($(wildcard $(F2C_BINARY)),)

else # ifdef F2C_BINARY 
	$(info F2C_BINARY is not set. Will use f2c sources from libdownloads/f2c)	

ifeq ($(wildcard ../../libdownloads/f2c/src),) 
	# unzip downloaded sources if libdownloads/f2c/src does not exist 
	(cd ../../libdownloads; mkdir -p f2c/src/libf2c; cd f2c/src/libf2c; unzip ../../libf2c.zip)
	(cd ../../libdownloads/f2c/src; tar xfz ../src.tgz; mv src f2c)
	# patch downloaded sources
	cp -v libf2c-patch/* ../../libdownloads/f2c/src/libf2c
endif 
	# make f2c
	# TODO: use -fPIC only on linux in libf2c.makefile
	$(MAKE) -f makefile.u -C ../../libdownloads/f2c/src/f2c CC=$(LABS_LLVM_CC)
	mkdir -p ../../bin
	cp ../../libdownloads/f2c/src/f2c/f2c ../../bin
	mkdir -p ../../f2c/
	cp -r ../../libdownloads/f2c/src/f2c ../../f2c/
	
	# make libf2c
	$(MAKE) -f makefile -C ../../libdownloads/f2c/src/libf2c CC=$(LABS_LLVM_CC)
	mkdir -p ../../f2c
	cp -r ../../libdownloads/f2c/src/libf2c ../../f2c/
endif # ifdef F2C_BINARY
	
	mkdir -p ../../lib
ifeq ($(OS_NAME),Darwin)
	cp ../../f2c/libf2c/libf2c.so ../../lib/libf2c.dylib;
	install_name_tool -id @rpath/libf2c.dylib ../../lib/libf2c.dylib
else	
	cp ../../f2c/libf2c/libf2c.so ../../lib/
endif
	cp ../../f2c/libf2c/f2c.h ../include


	
clean:
	# remove copied artefacts
	rm -f ../../bin/f2c
	rm -f ../../lib/libf2c.*
	rm -fr ../../f2c
	rm -fr ../include/f2c.h
	
	# clean build dir
	if [ -d ../../libdownloads/f2c/src/libf2c ]; then $(MAKE) -C ../../libdownloads/f2c/src/libf2c clean; fi
	if [ -d ../../libdownloads/f2c/src/f2c ]; then $(MAKE) -f makefile.u -C ../../libdownloads/f2c/src/f2c clean; fi
