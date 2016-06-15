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
#include <Rinterface.h>


static JavaVM *javaVM;
static JNIEnv *jniEnv;
static jobject engine;
static int initialized = 0;

static jclass rembeddedClass;
static jclass rStartParamsClass;
static jclass rInterfaceCallbacksClass;

int R_running_as_main_program;
int R_SignalHandlers;
FILE * R_Consolefile;
FILE * R_Outputfile;


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

// separate vm args from user args
static int process_vmargs(int argc, char *argv[], char *vmargv[], char *uargv[]) {
	int vcount = 0;
	int ucount = 0;
	for (int i = 0; i < argc; i++) {
		char *arg = argv[i];
		if (arg[0] == '-' && arg[1] == 'X') {
			vmargv[vcount++] = arg;
		} else {
			uargv[ucount++] = arg;
		}
	}
	return vcount;
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
	char *vm_cp = getenv("FASTR_CLASSPATH");
	if (vm_cp == NULL) {
		printf("Rf_initialize_R: FASTR_CLASSPATH env var not set\n");
		exit(1);
	}
	int cplen = (int) strlen(vm_cp);

	char *cp = malloc(cplen + 32);
	strcpy(cp, "-Djava.class.path=");
	strcat(cp, vm_cp);

	char **vmargs = malloc(argc * sizeof(char*));
	char **uargs = malloc(argc * sizeof(char*));
	int vmargc = process_vmargs(argc, argv, vmargs, uargs);
	argc -= vmargc;
	argv = uargs;
	JavaVMOption vm_options[1 + vmargc];

	vm_options[0].optionString = cp;
	for (int i = 0; i < vmargc; i++) {
		vm_options[i + 1].optionString = vmargs[i];
	}

	printf("creating vm\n");

	JavaVMInitArgs vm_args;
	vm_args.version = JNI_VERSION_1_8;
	vm_args.nOptions = 1 + vmargc;
	vm_args.options = vm_options;
	vm_args.ignoreUnrecognized = JNI_TRUE;

	jint flag = (*createJavaVMFunc)(&javaVM, (void**)
			&jniEnv, &vm_args);
	if (flag == JNI_ERR) {
		printf("Rf_initEmbeddedR: error creating Java VM, exiting...\n");
		return 1;
	}

	rInterfaceCallbacksClass = checkFindClass(jniEnv, "com/oracle/truffle/r/runtime/RInterfaceCallbacks");
	rembeddedClass = checkFindClass(jniEnv, "com/oracle/truffle/r/engine/shell/REmbedded");
	rStartParamsClass = checkFindClass(jniEnv, "com/oracle/truffle/r/runtime/RStartParams");
	jclass stringClass = checkFindClass(jniEnv, "java/lang/String");
	jmethodID initializeMethod = checkGetMethodID(jniEnv, rembeddedClass, "initializeR",
			"([Ljava/lang/String;)Lcom/oracle/truffle/api/vm/PolyglotEngine;", 1);
	jobjectArray argsArray = (*jniEnv)->NewObjectArray(jniEnv, argc, stringClass, NULL);
	for (int i = 0; i < argc; i++) {
		jstring arg = (*jniEnv)->NewStringUTF(jniEnv, argv[i]);
		(*jniEnv)->SetObjectArrayElement(jniEnv, argsArray, i, arg);
	}

	engine = checkRef(jniEnv, (*jniEnv)->CallStaticObjectMethod(jniEnv, rembeddedClass, initializeMethod, argsArray));
	initialized++;
	return 0;
}

void R_DefParams(Rstart rs) {
    // These are the GnuR defaults and correspond to the settings in RStartParams
	// None of the size params make any sense for FastR
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
	jmethodID setParamsMethodID = checkGetMethodID(jniEnv, rStartParamsClass, "setParams", "(ZZZZZZZIIZ)V", 1);
	(*jniEnv)->CallStaticVoidMethod(jniEnv, rStartParamsClass, setParamsMethodID, rs->R_Quiet, rs->R_Slave, rs->R_Interactive,
			rs->R_Verbose, rs->LoadSiteFile, rs->LoadInitFile, rs->DebugInitFile,
			rs->RestoreAction, rs->SaveAction, rs->NoRenviron);
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
	R_Interactive = TRUE;
    setup_Rmainloop();
    return 1;
}

void Rf_endEmbeddedR(int fatal) {
	(*javaVM)->DestroyJavaVM(javaVM);
	//TODO fatal
}

static void setupOverrides(void);

void setup_Rmainloop(void) {
	jmethodID setupMethod = checkGetMethodID(jniEnv, rembeddedClass, "setupRmainloop", "(Lcom/oracle/truffle/api/vm/PolyglotEngine;)V", 1);
	(*jniEnv)->CallStaticVoidMethod(jniEnv, rembeddedClass, setupMethod, engine);
}

void run_Rmainloop(void) {
	setupOverrides();
	jmethodID mainloopMethod = checkGetMethodID(jniEnv, rembeddedClass, "runRmainloop", "(Lcom/oracle/truffle/api/vm/PolyglotEngine;)V", 1);
	(*jniEnv)->CallStaticVoidMethod(jniEnv, rembeddedClass, mainloopMethod, engine);
}

void Rf_mainloop(void) {
	setup_Rmainloop();
	run_Rmainloop();
}

// functions that can be assigned by an embedded client to change behavior

void uR_Suicide(const char *x) {
	unimplemented("R_Suicide");
}

void uR_ShowMessage(const char *x) {
	unimplemented("R_ShowMessage");
}

int uR_ReadConsole(const char *a, unsigned char *b, int c, int d) {
	return (int) unimplemented("R_ReadConsole");
}

void uR_WriteConsole(const char *x, int y) {
	unimplemented("R_WriteConsole");
}

void uR_WriteConsoleEx(const char *x, int y, int z) {
	unimplemented("R_WriteConsole");
}

void uR_ResetConsole(void) {
	unimplemented("R_ResetConsole");
}

void uR_FlushConsole(void) {
	unimplemented("R_FlushConsole");
}

void uR_ClearerrConsole(void) {
	unimplemented("R_ClearerrConsole");
}

void uR_Busy(int x) {
	unimplemented("R_Busy");
}

void uR_CleanUp(SA_TYPE x, int y, int z) {
	unimplemented("R_CleanUp");
}

int uR_ShowFiles(int a, const char **b, const char **c,
	       const char *d, Rboolean e, const char *f) {
	return (int) unimplemented("R_ShowFiles");
}

int uR_ChooseFile(int a, char *b, int c) {
	return (int) unimplemented("R_ChooseFile");
}

int uR_EditFile(const char *a) {
	return (int) unimplemented("R_EditFile");
}

void uR_loadhistory(SEXP a, SEXP b, SEXP c, SEXP d) {
	unimplemented("uR_loadhistory");
}

void uR_savehistory(SEXP a, SEXP b, SEXP c, SEXP d) {
	unimplemented("R_savehistory");
}

void uR_addhistory(SEXP a, SEXP b, SEXP c, SEXP d) {
	unimplemented("R_addhistory");
}

int  uR_EditFiles(int a, const char **b, const char **c, const char *d) {
	return (int)unimplemented("");
}

SEXP udo_selectlist(SEXP a, SEXP b, SEXP c, SEXP d) {
	return unimplemented("R_EditFiles");
}

SEXP udo_dataentry(SEXP a, SEXP b, SEXP c, SEXP d) {
	return unimplemented("do_dataentry");
}

SEXP udo_dataviewer(SEXP a, SEXP b, SEXP c, SEXP d) {
	return unimplemented("do_dataviewer");
}

void uR_ProcessEvents(void) {
	unimplemented("R_ProcessEvents");
}


void (*ptr_R_Suicide)(const char *) = uR_Suicide;
void (*ptr_R_ShowMessage)(const char *) = uR_ShowMessage;
int  (*ptr_R_ReadConsole)(const char *, unsigned char *, int, int) = uR_ReadConsole;
void (*ptr_R_WriteConsole)(const char *, int) = uR_WriteConsole;
void (*ptr_R_WriteConsoleEx)(const char *, int, int) = uR_WriteConsoleEx;
void (*ptr_R_ResetConsole)(void) = uR_ResetConsole;
void (*ptr_R_FlushConsole)(void) = uR_FlushConsole;
void (*ptr_R_ClearerrConsole)(void) = uR_ClearerrConsole;
void (*ptr_R_Busy)(int) = uR_Busy;
void (*ptr_R_CleanUp)(SA_TYPE, int, int) = uR_CleanUp;
int  (*ptr_R_ShowFiles)(int, const char **, const char **,
			       const char *, Rboolean, const char *) = uR_ShowFiles;
int  (*ptr_R_ChooseFile)(int, char *, int) = uR_ChooseFile;
int  (*ptr_R_EditFile)(const char *) = uR_EditFile;
void (*ptr_R_loadhistory)(SEXP, SEXP, SEXP, SEXP) = uR_loadhistory;
void (*ptr_R_savehistory)(SEXP, SEXP, SEXP, SEXP) = uR_savehistory;
void (*ptr_R_addhistory)(SEXP, SEXP, SEXP, SEXP) = uR_addhistory;

int  (*ptr_R_EditFiles)(int, const char **, const char **, const char *) = uR_EditFiles;

SEXP (*ptr_do_selectlist)(SEXP, SEXP, SEXP, SEXP) = udo_selectlist;
SEXP (*ptr_do_dataentry)(SEXP, SEXP, SEXP, SEXP) = udo_dataentry;
SEXP (*ptr_do_dataviewer)(SEXP, SEXP, SEXP, SEXP) = udo_dataviewer;
void (*ptr_R_ProcessEvents)() = uR_ProcessEvents;

void setupOverrides(void) {
	jmethodID ovrMethodID = checkGetMethodID(jniEnv, rInterfaceCallbacksClass, "override", "(Ljava/lang/String;)V", 1);
	jstring name;
	if (ptr_R_Suicide != uR_Suicide) {
		name = (*jniEnv)->NewStringUTF(jniEnv, "R_Suicide");
		(*jniEnv)->CallStaticVoidMethod(jniEnv, rInterfaceCallbacksClass, ovrMethodID, name);
	}
	if (*ptr_R_CleanUp != uR_CleanUp) {
		name = (*jniEnv)->NewStringUTF(jniEnv, "R_CleanUp");
		(*jniEnv)->CallStaticVoidMethod(jniEnv, rInterfaceCallbacksClass, ovrMethodID, name);
	}
	if (*ptr_R_ReadConsole != uR_ReadConsole) {
		name = (*jniEnv)->NewStringUTF(jniEnv, "R_ReadConsole");
		(*jniEnv)->CallStaticVoidMethod(jniEnv, rInterfaceCallbacksClass, ovrMethodID, name);
	}
	if (*ptr_R_WriteConsole != uR_WriteConsole) {
		name = (*jniEnv)->NewStringUTF(jniEnv, "R_WriteConsole");
		(*jniEnv)->CallStaticVoidMethod(jniEnv, rInterfaceCallbacksClass, ovrMethodID, name);
	}
}

static void REmbed_nativeWriteConsole(JNIEnv *env, jclass c, jstring string, int otype) {
	int len = (*jniEnv)->GetStringUTFLength(jniEnv, string);
	const char *cbuf =  (*jniEnv)->GetStringUTFChars(jniEnv, string, NULL);
	if (ptr_R_WriteConsole == NULL) {
		(*ptr_R_WriteConsoleEx)(cbuf, len, otype);
	} else {
	    (*ptr_R_WriteConsole)(cbuf, len);
	}
	(*jniEnv)->ReleaseStringUTFChars(jniEnv, string, cbuf);
}

JNIEXPORT void JNICALL Java_com_oracle_truffle_r_runtime_ffi_jnr_JNI_1REmbed_nativeWriteConsole(JNIEnv *env, jclass c, jstring string) {
	REmbed_nativeWriteConsole(jniEnv, c, string, 0);
}

JNIEXPORT void JNICALL Java_com_oracle_truffle_r_runtime_ffi_jnr_JNI_1REmbed_nativeWriteErrConsole(JNIEnv *env, jclass c, jstring string) {
	REmbed_nativeWriteConsole(jniEnv, c, string, 1);
}



JNIEXPORT jstring JNICALL Java_com_oracle_truffle_r_runtime_ffi_jnr_JNI_1REmbed_nativeReadConsole(JNIEnv *env, jclass c, jstring prompt) {
	const char *cprompt =  (*jniEnv)->GetStringUTFChars(jniEnv, prompt, NULL);
	unsigned char cbuf[1024];
	int n = (*ptr_R_ReadConsole)(cprompt, cbuf, 1024, 0);
	jstring result;
	result = (*jniEnv)->NewStringUTF(jniEnv, (const char *)cbuf);
	(*jniEnv)->ReleaseStringUTFChars(jniEnv, prompt, cprompt);
	return result;
}

