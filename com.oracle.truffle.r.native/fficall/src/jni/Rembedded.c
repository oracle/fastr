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
#include <sys/stat.h>
#include <rffiutils.h>
#define R_INTERFACE_PTRS
#include <Rinterface.h>
#include <R_ext/RStartup.h>

extern char **environ;

static JavaVM *javaVM;
static jobject engine;
static int initialized = 0;
static char *java_home;

static jclass rembeddedClass;
static jclass rStartParamsClass;
static jclass rInterfaceCallbacksClass;

int R_running_as_main_program;
int R_SignalHandlers;
FILE * R_Consolefile;
FILE * R_Outputfile;
int R_DirtyImage; // TODO update this
void *R_GlobalContext; // TODO what?
SA_TYPE SaveAction; // ??

typedef jint (JNICALL *JNI_CreateJavaVMFunc)
	      (JavaVM **pvm, void **penv, void *args);


static void *dlopen_jvmlib(char *libpath) {
	void *handle = dlopen(libpath, RTLD_GLOBAL | RTLD_NOW);
	if (handle == NULL) {
		fprintf(stderr, "Rf_initialize_R: cannot dlopen %s: %s\n", libpath, dlerror());
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
		if ((arg[0] == '-' && arg[1] == 'X') || (arg[0] == '-' && arg[1] == 'D')) {
			vmargv[vcount++] = arg;
		} else {
			uargv[ucount++] = arg;
		}
	}
	return vcount;
}

static char **update_environ_with_java_home(void);
static void print_environ(char **env);
static char *get_classpath(char *r_home);

# define JMP_BUF sigjmp_buf

int Rf_initialize_R(int argc, char *argv[]) {
	if (initialized) {
		fprintf(stderr, "%s", "R is already initialized\n");
		exit(1);
	}
	// print_environ(environ);
	char *r_home = getenv("R_HOME");
	if (r_home == NULL) {
		fprintf(stderr, "R_HOME must be set\n");
		exit(1);
	}
	struct utsname utsname;
	uname(&utsname);
	char jvmlib_path[256];
	java_home = getenv("JAVA_HOME");
	if (java_home == NULL) {
		if (strcmp(utsname.sysname, "Linux") == 0) {
			char *jvmdir = "/usr/java/latest";
			struct stat statbuf;
			if (stat(jvmdir, &statbuf) == 0) {
				java_home = jvmdir;
			}
		} else if (strcmp(utsname.sysname, "Darwin") == 0) {
			char *jvmdir = "/Library/Java/JavaVirtualMachines/jdk.latest";
			struct stat statbuf;
			if (stat(jvmdir, &statbuf) == 0) {
				java_home = (char*)malloc(strlen(jvmdir) + 32);
				strcpy(java_home, jvmdir);
				strcat(java_home, "/Contents/Home");
			}
		}
		if (java_home == NULL) {
			fprintf(stderr, "Rf_initialize_R: can't find a JAVA_HOME\n");
			exit(1);
		}
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
		fprintf(stderr, "unsupported OS: %s\n", utsname.sysname);
		exit(1);
	}
	void *vm_handle = dlopen_jvmlib(jvmlib_path);
	JNI_CreateJavaVMFunc createJavaVMFunc = (JNI_CreateJavaVMFunc) dlsym(vm_handle, "JNI_CreateJavaVM");
	if (createJavaVMFunc == NULL) {
		fprintf(stderr, "Rf_initialize_R: cannot find JNI_CreateJavaVM\n");
		exit(1);
	}

	char *vm_cp = get_classpath(r_home);
	//printf("cp %s\n", vm_cp);

	char **vmargs = malloc(argc * sizeof(char*));
	char **uargs = malloc(argc * sizeof(char*));
	int vmargc = process_vmargs(argc, argv, vmargs, uargs);
	argc -= vmargc;
	argv = uargs;
	JavaVMOption vm_options[1 + vmargc];

	vm_options[0].optionString = vm_cp;
	for (int i = 0; i < vmargc; i++) {
		vm_options[i + 1].optionString = vmargs[i];
	}

	JavaVMInitArgs vm_args;
	vm_args.version = JNI_VERSION_1_8;
	vm_args.nOptions = 1 + vmargc;
	vm_args.options = vm_options;
	vm_args.ignoreUnrecognized = JNI_TRUE;

	JNIEnv *jniEnv;
	jint flag = (*createJavaVMFunc)(&javaVM, (void**)
			&jniEnv, &vm_args);
	if (flag == JNI_ERR) {
		fprintf(stderr, "Rf_initEmbeddedR: error creating Java VM, exiting...\n");
		return 1;
	}

	setEmbedded();
	setEnv(jniEnv);
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
	// Can't TRACE this upcall as system not initialized
	engine = checkRef(jniEnv, (*jniEnv)->CallStaticObjectMethod(jniEnv, rembeddedClass, initializeMethod, argsArray));
	initialized++;
	return 0;
}

char *R_HomeDir(void) {
	JNIEnv *jniEnv = getEnv();
	jmethodID R_HomeDirMethodID = checkGetMethodID(jniEnv, UpCallsRFFIClass, "R_HomeDir", "()Ljava/lang/String;", 0);
	jstring homeDir = (*jniEnv)->CallObjectMethod(jniEnv, UpCallsRFFIObject, R_HomeDirMethodID);
	const char *homeDirChars = stringToChars(jniEnv, homeDir);
	return (char *)homeDirChars;
}

void R_SaveGlobalEnvToFile(const char *f) {
	unimplemented("R_SaveGlobalEnvToFile");
}

void R_Suicide(const char *s) { ptr_R_Suicide(s); }


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
	JNIEnv *jniEnv = getEnv();
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
	JNIEnv *jniEnv = getEnv();
	jmethodID setupMethod = checkGetMethodID(jniEnv, rembeddedClass, "setupRmainloop", "(Lcom/oracle/truffle/api/vm/PolyglotEngine;)V", 1);
	(*jniEnv)->CallStaticVoidMethod(jniEnv, rembeddedClass, setupMethod, engine);
}

void run_Rmainloop(void) {
	JNIEnv *jniEnv = getEnv();
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
	JNIEnv *jniEnv = getEnv();
	jstring msg = (*jniEnv)->NewStringUTF(jniEnv, x);
	jmethodID suicideMethod = checkGetMethodID(jniEnv, rembeddedClass, "R_Suicide", "(Ljava/lang/String;)V", 1);
	(*jniEnv)->CallStaticVoidMethod(jniEnv, rembeddedClass, suicideMethod, msg);
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
	JNIEnv *jniEnv = getEnv();
	jmethodID methodID = checkGetMethodID(jniEnv, UpCallsRFFIClass, "R_CleanUp", "(III)V", 1);
	(*jniEnv)->CallStaticVoidMethod(jniEnv, UpCallsRFFIClass, methodID, x, y, z);
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
	JNIEnv *jniEnv = getEnv();
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

static void REmbed_nativeWriteConsole(JNIEnv *jniEnv, jclass c, jstring string, int otype) {
	jmp_buf error_jmpbuf;
	callEnter(jniEnv, &error_jmpbuf);
	if (!setjmp(error_jmpbuf)) {
		int len = (*jniEnv)->GetStringUTFLength(jniEnv, string);
		const char *cbuf =  (*jniEnv)->GetStringUTFChars(jniEnv, string, NULL);
		if (ptr_R_WriteConsole == NULL) {
			(*ptr_R_WriteConsoleEx)(cbuf, len, otype);
		} else {
			(*ptr_R_WriteConsole)(cbuf, len);
		}
		(*jniEnv)->ReleaseStringUTFChars(jniEnv, string, cbuf);
	}
	callExit(jniEnv);
}

JNIEXPORT void JNICALL Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1REmbed_nativeWriteConsole(JNIEnv *jniEnv, jclass c, jstring string) {
	REmbed_nativeWriteConsole(jniEnv, c, string, 0);
}

JNIEXPORT void JNICALL Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1REmbed_nativeWriteErrConsole(JNIEnv *jniEnv, jclass c, jstring string) {
	REmbed_nativeWriteConsole(jniEnv, c, string, 1);
}

JNIEXPORT jstring JNICALL Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1REmbed_nativeReadConsole(JNIEnv *jniEnv, jclass c, jstring prompt) {
	jmp_buf error_jmpbuf;
	jstring result = NULL;
	callEnter(jniEnv, &error_jmpbuf);
	if (!setjmp(error_jmpbuf)) {
		const char *cprompt =  (*jniEnv)->GetStringUTFChars(jniEnv, prompt, NULL);
		unsigned char cbuf[1024];
		int n = (*ptr_R_ReadConsole)(cprompt, cbuf, 1024, 0);
		result = (*jniEnv)->NewStringUTF(jniEnv, (const char *)cbuf);
		(*jniEnv)->ReleaseStringUTFChars(jniEnv, prompt, cprompt);
	}
	callExit(jniEnv);
	return result;
}

JNIEXPORT void JNICALL Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1REmbed_nativeCleanUp(JNIEnv *jniEnv, jclass c, jint x, jint y, jint z) {
	jmp_buf error_jmpbuf;
	callEnter(jniEnv, &error_jmpbuf);
	if (!setjmp(error_jmpbuf)) {
	(*ptr_R_CleanUp)(x, y, z);
	}
	callExit(jniEnv);
}

JNIEXPORT void JNICALL Java_com_oracle_truffle_r_runtime_ffi_jni_JNI_1REmbed_nativeSuicide(JNIEnv *jniEnv, jclass c, jstring string) {
	jmp_buf error_jmpbuf;
	callEnter(jniEnv, &error_jmpbuf);
	if (!setjmp(error_jmpbuf)) {
		const char *cbuf =  (*jniEnv)->GetStringUTFChars(jniEnv, string, NULL);
		(*ptr_R_Suicide)(cbuf);
	}
	callExit(jniEnv);
}

void uR_PolledEvents(void) {
	unimplemented("R_PolledEvents");
}

void (* R_PolledEvents)(void) = uR_PolledEvents;

void Rf_jump_to_toplevel() {
	unimplemented("Rf_jump_to_toplevel");
}

#include <R_ext/eventloop.h>

fd_set *R_checkActivity(int usec, int ignore_stdin) {
	return (fd_set*) unimplemented("R_checkActivity");
}

void R_runHandlers(InputHandler *handlers, fd_set *mask) {
	unimplemented("R_runHandlers");
}

int R_wait_usec;

#include <unistd.h>
#include <errno.h>

static void perror_exit(char *msg) {
	perror(msg);
	exit(1);
}

// support for getting the correct classpath for the VM
// We use $R_HOME/bin/execRextras/Rclasspath to do this to emulate what happens
// during normal execution
static char *get_classpath(char *r_home) {
	char **env = update_environ_with_java_home();
	//print_environ(env);
	int pipefd[2];
	if (pipe(pipefd) == -1) {
		perror_exit("pipe");
	}
	pid_t pid = fork();
	if (pid == -1) {
		perror("fork");
	}
	if (pid == 0) {
		// child
		char path[1024];
		strcpy(path, r_home);
		strcat(path, "/bin/execRextras/Rclasspath");
		while ((dup2(pipefd[1], STDOUT_FILENO) == -1) && (errno == EINTR)) {}
		close(pipefd[1]);
		close(pipefd[0]);
		int rc = execle(path, path, (char *)NULL, env);
		if (rc == -1) {
			perror_exit("exec");
		}
		return NULL;
	} else {
		// parent
		const char *cpdef = "-Djava.class.path=";
		char *buf = malloc(4096);
		strcpy(buf, cpdef);
		char *bufptr = buf + strlen(cpdef);
		int max = 4096 - strlen(cpdef);
		close(pipefd[1]);
		while (1) {
			int count = read(pipefd[0], bufptr, max);
			if (count == -1) {
				if (errno == EINTR) {
					continue;
				} else {
					perror_exit("read");
				}
			} else if (count == 0) {
			    // scrub any newline
			    bufptr--;
			    if (*bufptr != '\n') {
			        bufptr++;
			    }
				*bufptr = 0;
				break;
			} else {
				bufptr += count;
				max -= count;
			}
		}
		close(pipefd[0]);
		wait(NULL);
		return buf;
	}
}

// debugging
static void print_environ(char **env) {
	fprintf(stdout, "## Environment variables at %p\n", env);
	char **e = env;
	while (*e != NULL) {
		fprintf(stdout, "%s\n", *e);
		e++;
	}
}

static char **update_environ(char *def) {
	int count = 0;
	char **e = environ;
	while (*e != NULL) {
		e++;
		count++;
	}
	char **new_env = malloc(sizeof(char *) * (count + 2));
	e = environ;
	char **ne = new_env;
	while (*e != NULL) {
		*ne++ = *e++;
	}
	*ne++ = def;
	*ne = (char*) NULL;
	return new_env;
}

static char **update_environ_with_java_home(void) {
	char **e = environ;
	while (*e != NULL) {
		if (strstr(*e, "JAVA_HOME=")) {
			return environ;
		}
		e++;
	}
	char *java_home_env = malloc(strlen(java_home) + 10);
	strcpy(java_home_env, "JAVA_HOME=");
	strcat(java_home_env, java_home);
	return update_environ(java_home_env);
}

CTXT R_getGlobalFunctionContext() {
	JNIEnv *jniEnv = getEnv();
	jmethodID methodID = checkGetMethodID(jniEnv, UpCallsRFFIClass, "R_getGlobalFunctionContext", "()Ljava/lang/Object;", 0);
    CTXT result = (*jniEnv)->CallObjectMethod(jniEnv, UpCallsRFFIObject, methodID);
    SEXP new_result = checkRef(jniEnv, result);
    return new_result == R_NilValue ? NULL : addGlobalRef(jniEnv, result, 0);
}

CTXT R_getParentFunctionContext(CTXT c) {
	JNIEnv *jniEnv = getEnv();
	jmethodID methodID = checkGetMethodID(jniEnv, UpCallsRFFIClass, "R_getParentFunctionContext", "(Ljava/lang/Object;)Ljava/lang/Object;", 0);
    CTXT result = (*jniEnv)->CallObjectMethod(jniEnv, UpCallsRFFIObject, methodID, c);
    SEXP new_result = checkRef(jniEnv, result);
    return new_result == R_NilValue ? NULL : addGlobalRef(jniEnv, result, 0);
}

SEXP R_getContextEnv(CTXT context) {
	JNIEnv *jniEnv = getEnv();
	jmethodID methodID = checkGetMethodID(jniEnv, UpCallsRFFIClass, "R_getContextEnv", "(Ljava/lang/Object;)Ljava/lang/Object;", 0);
    SEXP result = (*jniEnv)->CallObjectMethod(jniEnv, UpCallsRFFIObject, methodID, context);
    return checkRef(jniEnv, result);
}

SEXP R_getContextFun(CTXT context) {
	JNIEnv *jniEnv = getEnv();
	jmethodID methodID = checkGetMethodID(jniEnv, UpCallsRFFIClass, "R_getContextFun", "(Ljava/lang/Object;)Ljava/lang/Object;", 0);
    SEXP result = (*jniEnv)->CallObjectMethod(jniEnv, UpCallsRFFIObject, methodID, context);
    return checkRef(jniEnv, result);
}

SEXP R_getContextCall(CTXT context) {
	JNIEnv *jniEnv = getEnv();
	jmethodID methodID = checkGetMethodID(jniEnv, UpCallsRFFIClass, "R_getContextCall", "(Ljava/lang/Object;)Ljava/lang/Object;", 0);
    SEXP result = (*jniEnv)->CallObjectMethod(jniEnv, UpCallsRFFIObject, methodID, context);
    return checkRef(jniEnv, result);
}

SEXP R_getContextSrcRef(CTXT context) {
    JNIEnv *jniEnv = getEnv();
    jmethodID methodID = checkGetMethodID(jniEnv, UpCallsRFFIClass, "R_getContextSrcRef", "(Ljava/lang/Object;)Ljava/lang/Object;", 0);
    SEXP result = (*jniEnv)->CallObjectMethod(jniEnv, UpCallsRFFIObject, methodID, context);
    result = checkRef(jniEnv, result);
    return result == R_NilValue ? NULL : result;
}

int R_insideBrowser() {
	JNIEnv *jniEnv = getEnv();
	jmethodID methodID = checkGetMethodID(jniEnv, UpCallsRFFIClass, "R_insideBrowser", "()I", 0);
    return (*jniEnv)->CallStaticIntMethod(jniEnv, UpCallsRFFIClass, methodID);
}

int R_isGlobal(CTXT context) {
	JNIEnv *jniEnv = getEnv();
	jmethodID methodID = checkGetMethodID(jniEnv, UpCallsRFFIClass, "R_isGlobal", "(Ljava/lang/Object;)I", 0);
    return (*jniEnv)->CallStaticIntMethod(jniEnv, UpCallsRFFIClass, methodID, context);
}

int R_isEqual(void* x, void* y) {
	JNIEnv *jniEnv = getEnv();
	jmethodID methodID = checkGetMethodID(jniEnv, UpCallsRFFIClass, "R_isEqual", "(Ljava/lang/Object;Ljava/lang/Object;)I", 0);
    return (*jniEnv)->CallStaticIntMethod(jniEnv, UpCallsRFFIClass, methodID, x, y);
}
