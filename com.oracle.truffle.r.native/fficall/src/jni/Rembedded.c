/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
#include <rffiutils.h>
#include <dlfcn.h>
#include <sys/utsname.h>

static JavaVM *javaVM;

int Rf_initEmbeddedR(int argc, char *argv[]) {
	struct utsname utsname;
	uname(&utsname);
	printf("is: %s\n", utsname.sysname);
	char *jvmlib_path = malloc(256);
	char *fastr_java_vm = getenv("FASTR_JAVA_VM");
	if (fastr_java_vm == NULL) {
		char *java_home = getenv("JAVA_HOME");
		if (java_home == NULL) {
			printf("Rf_initEmbeddedR: can't find a Java VM");
			exit(1);
		}
		strcpy(jvmlib_path, java_home);
		if (strcmp(utsname.sysname, "Linux")) {
			strcat(jvmlib_path, "/jre/lib/amd64/server/libjvm.so");
		} else if (strcmp(utsname.sysname, "Darwin")) {
			strcat(jvmlib_path, "/jre/lib/server/libjvm.dylib");
		} else {
			printf("unsupported OS: %s\n"utsname.sysname);
			exit(1);
		}
	} else {
		strcpy(jvmlib_path, fastr_java_vm);
	}
	void *vm_handle = dlopen(jvmlib_path, RTLD_GLOBAL | RTLD_NOW);
	if (vm_handle == NULL) {
		printf("Rf_initEmbeddedR: cannot dlopen %s: %s\n", jvmlib_path, dlerror());
		exit(1);
	}

	// TODO getting the correct classpath is hard, need a helper program
	JavaVMOption vm_options[1];
	char *vm_cp = getenv("FASTR_CLASSPATH");
	if (vm_cp == NULL) {
		printf("Rf_initEmbeddedR: FASTR_CLASSPATH env var not set\n");
		exit(1);
	}
	int cplen = (int) strlen(vm_cp);

	char *cp = malloc(cplen + 32);
	strcpy(cp, "-Djava.class.path=");
	strcat(cp, vm_cp);
	vm_options[0].optionString = cp;

	printf("creating vm\n");

	JavaVMInitArgs vm_args;
	vm_args.version = JNI_VERSION_1_8;
	vm_args.nOptions = 1;
	vm_args.options = vm_options;
	vm_args.ignoreUnrecognized = JNI_TRUE;

	JNIEnv *jniEnv;
	long flag = JNI_CreateJavaVM(&javaVM, (void**)
			&jniEnv, &vm_args);
	if (flag == JNI_ERR) {
		printf("Rf_initEmbeddedR: error creating VM, exiting...\n");
		return 1;
	}
	jclass mainClass = checkFindClass(jniEnv, "com/oracle/truffle/r/engine/shell/RCommand");
	jclass stringClass = checkFindClass(jniEnv, "java/lang/String");
	jmethodID mainMethod = checkGetMethodID(jniEnv, mainClass, "main", "([Ljava/lang/String;)V", 1);
	int argLength = 0;
	jobjectArray argsArray = (*jniEnv)->NewObjectArray(jniEnv, argLength, stringClass, NULL);

	(*jniEnv)->CallStaticVoidMethod(jniEnv, mainClass, mainMethod, argsArray);
	return 0;
}

void Rf_endEmbeddedR(int fatal) {
	(*javaVM)->DestroyJavaVM(javaVM);
	//TODO fatal
}
