/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
#include <dlfcn.h>
#include <sys/utsname.h>
#include <rffiutils.h>
#include <R_ext/RStartup.h>


static JavaVM *javaVM;
static JNIEnv *jniEnv;
static jobject engine;
static int initialized = 0;

static jclass rcommandClass;
static jclass rStartParamsClass;

typedef jint (JNICALL *JNI_CreateJavaVMFunc)
	      (JavaVM **pvm, void **penv, void *args);

static void *dlopen_jvmlib(char *libpath) {
	void *handle = dlopen(libpath, RTLD_GLOBAL | RTLD_NOW);
	if (handle == NULL) {
		printf("Rf_initialize_R: cannot dlopen %s: %s\n", libpath, dlerror());
		exit(1);
	}
	return handle;
}

int Rf_initialize_R(int argc, char *argv[]) {
	if (initialized) {
		fprintf(stderr, "%s", "R is already initialized\n");
		exit(1);
	}
	struct utsname utsname;
	uname(&utsname);
	char jvmlib_path[256];
	char *java_home = getenv("JAVA_HOME");
	if (java_home == NULL) {
		printf("Rf_initialize_R: can't find JAVA_HOME");
		exit(1);
	}
	strcpy(jvmlib_path, java_home);
	if (strcmp(utsname.sysname, "Linux") == 0) {
		strcat(jvmlib_path, "/jre/lib/amd64/server/libjvm.so");
	} else if (strcmp(utsname.sysname, "Darwin") == 0) {
		strcat(jvmlib_path, "/jre/lib/server/libjvm.dylib");
        // Must also load libjli to avoid going through framework
		// and failing to find our JAVA_HOME runtime
		char jlilib_path[256];
		strcpy(jlilib_path, java_home);
		strcat(jlilib_path, "/jre/lib/jli/libjli.dylib");
		dlopen_jvmlib(jlilib_path);
	} else {
		printf("unsupported OS: %s\n", utsname.sysname);
		exit(1);
	}
	void *vm_handle = dlopen_jvmlib(jvmlib_path);
	JNI_CreateJavaVMFunc createJavaVMFunc = (JNI_CreateJavaVMFunc) dlsym(vm_handle, "JNI_CreateJavaVM");
	if (createJavaVMFunc == NULL) {
		printf("Rf_initialize_R: cannot find JNI_CreateJavaVM\n");
		exit(1);
	}

	// TODO getting the correct classpath is hard, need a helper program
	JavaVMOption vm_options[1];
	char *vm_cp = getenv("FASTR_CLASSPATH");
	if (vm_cp == NULL) {
		printf("Rf_initialize_R: FASTR_CLASSPATH env var not set\n");
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

	jint flag = (*createJavaVMFunc)(&javaVM, (void**)
			&jniEnv, &vm_args);
	if (flag == JNI_ERR) {
		printf("Rf_initEmbeddedR: error creating Java VM, exiting...\n");
		return 1;
	}

	rcommandClass = checkFindClass(jniEnv, "com/oracle/truffle/r/engine/shell/RCommand");
	rStartParamsClass = checkFindClass(jniEnv, "com/oracle/truffle/r/engine/shell/RStartParams");
	jclass stringClass = checkFindClass(jniEnv, "java/lang/String");
	jmethodID initializeMethod = checkGetMethodID(jniEnv, rcommandClass, "initialize",
			"([Ljava/lang/String;)Lcom/oracle/truffle/api/vm/PolyglotEngine;", 1);
	int argLength = 0;
	jobjectArray argsArray = (*jniEnv)->NewObjectArray(jniEnv, argLength, stringClass, NULL);

	engine = checkRef(jniEnv, (*jniEnv)->CallStaticObjectMethod(jniEnv, rcommandClass, initializeMethod, argsArray));
	initialized++;
	return 0;
}

void setup_Rmainloop(void) {
	// In GnuR R_initialize_R and setup_Rmainloop do different things.
	// In FastR we don't (yet?) have the distinction, so there is nothing more to do here
}

void R_DefParams(Rstart rs) {
//	jmethodID getActiveMethod = checkGetMethodId(jniEnv, rStartParamsClass, "getActive", "(V)Lcom/oracle/truffle/r/engine/shell/RStartParams;", 1);
    rs->R_Quiet = FALSE;
    rs->R_Slave = FALSE;
    rs->R_Interactive = TRUE;
    rs->R_Verbose = FALSE;
    rs->RestoreAction = SA_RESTORE;
    rs->SaveAction = SA_SAVEASK;
    rs->LoadSiteFile = TRUE;
    rs->LoadInitFile = TRUE;
    rs->DebugInitFile = FALSE;
//    rs->vsize = R_VSIZE;
//    rs->nsize = R_NSIZE;
//    rs->max_vsize = R_SIZE_T_MAX;
//    rs->max_nsize = R_SIZE_T_MAX;
//    rs->ppsize = R_PPSSIZE;
    rs->NoRenviron = FALSE;
//    R_SizeFromEnv(Rp);
}

void R_SetParams(Rstart rs) {
	jmethodID setParamsMethodID = checkGetMethodID(jniEnv, rStartParamsClass, "setParams", "(Lcom/oracle/truffle/r/engine/shell/RStartParams;)V", 1);
	jmethodID consMethodID = checkGetMethodID(jniEnv, rStartParamsClass, "<init>", "(ZZZZZZZIIZ)V", 0);
	jobject javaRSParams = (*jniEnv)->NewObject(jniEnv, rStartParamsClass, consMethodID, rs->R_Quiet, rs->R_Slave, rs->R_Interactive,
			rs->R_Verbose, rs->LoadSiteFile, rs->LoadInitFile, rs->DebugInitFile,
			rs->RestoreAction, rs->SaveAction, rs->NoRenviron);
	(*jniEnv)->CallStaticVoidMethod(jniEnv, rStartParamsClass, setParamsMethodID, javaRSParams);
}

void R_SizeFromEnv(Rstart rs) {
	unimplemented("R_SizeFromEnv");
}

void R_common_command_line(int *a, char **b, Rstart rs) {
	unimplemented("R_common_command_line");
}

void R_set_command_line_arguments(int argc, char **argv) {
	unimplemented("R_set_command_line_arguments");
}


int Rf_initEmbeddedR(int argc, char *argv[]) {
	Rf_initialize_R(argc, argv);
//	R_Interactive = TRUE;
    setup_Rmainloop();
    return 1;
}

void Rf_endEmbeddedR(int fatal) {
	(*javaVM)->DestroyJavaVM(javaVM);
	//TODO fatal
}

void Rf_mainloop(void) {
	jmethodID readEvalPrintMethod = checkGetMethodID(jniEnv, rcommandClass, "readEvalPrint", "(Lcom/oracle/truffle/api/vm/PolyglotEngine;)V", 1);
	(*jniEnv)->CallStaticVoidMethod(jniEnv, rcommandClass, readEvalPrintMethod, engine);
}

// function ptrs that can be assigned by an embedded client to change behavior
void (*ptr_R_Suicide)(const char *);
void (*ptr_R_ShowMessage)(const char *);
int  (*ptr_R_ReadConsole)(const char *, unsigned char *, int, int);
void (*ptr_R_WriteConsole)(const char *, int);
void (*ptr_R_WriteConsoleEx)(const char *, int, int);
void (*ptr_R_ResetConsole)(void);
void (*ptr_R_FlushConsole)(void);
void (*ptr_R_ClearerrConsole)(void);
void (*ptr_R_Busy)(int);
void (*ptr_R_CleanUp)(SA_TYPE, int, int);
int  (*ptr_R_ShowFiles)(int, const char **, const char **,
			       const char *, Rboolean, const char *);
int  (*ptr_R_ChooseFile)(int, char *, int);
int  (*ptr_R_EditFile)(const char *);
void (*ptr_R_loadhistory)(SEXP, SEXP, SEXP, SEXP);
void (*ptr_R_savehistory)(SEXP, SEXP, SEXP, SEXP);
void (*ptr_R_addhistory)(SEXP, SEXP, SEXP, SEXP);

int  (*ptr_R_EditFiles)(int, const char **, const char **, const char *);

SEXP (*ptr_do_selectlist)(SEXP, SEXP, SEXP, SEXP);
SEXP (*ptr_do_dataentry)(SEXP, SEXP, SEXP, SEXP);
SEXP (*ptr_do_dataviewer)(SEXP, SEXP, SEXP, SEXP);
void (*ptr_R_ProcessEvents)();

