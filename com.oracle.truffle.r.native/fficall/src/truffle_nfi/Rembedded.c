/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

#include <dlfcn.h>
#include <sys/utsname.h>
#include <sys/stat.h>
#define R_INTERFACE_PTRS
#include <Rinterface.h>
#include <rffiutils.h>
#include <R_ext/RStartup.h>
#include <jni.h> 
#include "../common/rffi_upcalls.h"

// R_Interactive is actually field in the startup parameters structure, moreover, we'll also
// set-up the value of the actual external global R_Interactive exported and supported
// (in single threaded mode) by FastR.
#undef R_Interactive

extern char **environ;

static JavaVM *javaVM;
static JNIEnv *jniEnv = NULL;
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

int R_wait_usec;    // TODO: necessary to resolve externals? otherwise dead code

typedef jint (JNICALL *JNI_CreateJavaVMFunc)(JavaVM **pvm, void **penv, void *args);


// ------------------------------------------------------
// Forward declarations of static helper functions

static void setupOverrides(void);

static void *dlopen_jvmlib(char *libpath);
static JNIEnv* getEnv();
static jmethodID checkGetMethodID(JNIEnv *env, jclass klass, const char *name, const char *sig, int isStatic);
static jclass checkFindClass(JNIEnv *env, const char *name);
static int process_vmargs(int argc, char *argv[], char *vmargv[], char *uargv[]);
static char **update_environ_with_java_home(void);
static void print_environ(char **env);
static char *get_classpath(char *r_home);


// ----------------------------------------------------------------------------------
// ----------------------------------------------------------------------------------
// The embedding API
// So far it seems that GNU R can be embedded by invoking either:
//      1) Rf_initEmbeddedR: initializing the main loop without actually running it after which the user
//          can e.g. eval code using Rf_eval, finish with Rf_endEmbeddedR
//      2) Rf_initialize_R & Rf_mainloop: initializing and runnning the main loop

// Does the heavy work of starting up the JVM and invoking REmbedded#initializeR, which should be the
// only upcall to Java made via JNI. If setupRmainloop != 0, then also initialized the main loop (does not run it).
static int initializeFastR(int argc, char *argv[], int setupRmainloop);

// initializes R, but user is expected (TODO: probably) to invoke the main loop after that
int Rf_initialize_R(int argc, char *argv[]) {
	return initializeFastR(argc, argv, 0);
}

// initializes R and the main loop without running it
int Rf_initEmbeddedR(int argc, char *argv[]) {
	return initializeFastR(argc, argv, 1);
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
    rs->NoRenviron = FALSE;
}

// Allows to set-up some params before the main loop is initialized
// This call has to be made via JNI as we are not in a down-call, i.e. in truffle context, when this gets executed.
void R_SetParams(Rstart rs) {
	JNIEnv *jniEnv = getEnv();
	jmethodID setParamsMethodID = checkGetMethodID(jniEnv, rembeddedClass, "setParams", "(ZZZZZZZIIZ)V", 1);
	(*jniEnv)->CallStaticVoidMethod(jniEnv, rStartParamsClass, setParamsMethodID, rs->R_Quiet, rs->R_Slave, rs->R_Interactive,
			rs->R_Verbose, rs->LoadSiteFile, rs->LoadInitFile, rs->DebugInitFile,
			rs->RestoreAction, rs->SaveAction, rs->NoRenviron);
}

// Runs the main REPL loop
void Rf_mainloop(void) {
	JNIEnv *jniEnv = getEnv();
	setupOverrides();
	jmethodID mainloopMethod = checkGetMethodID(jniEnv, rembeddedClass, "runRmainloop", "()V", 1);
	(*jniEnv)->CallStaticVoidMethod(jniEnv, rembeddedClass, mainloopMethod);
}

void R_Suicide(const char *s) { ptr_R_Suicide(s); }

void Rf_endEmbeddedR(int fatal) {
    // TODO: invoke com.oracle.truffle.r.engine.shell.REmbedded#endRmainloop
	(*javaVM)->DestroyJavaVM(javaVM);
	//TODO fatal
}

static int initializeFastR(int argc, char *argv[], int setupRmainloop) {
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

	jint flag = (*createJavaVMFunc)(&javaVM, (void**)
			&jniEnv, &vm_args);
	if (flag == JNI_ERR) {
		fprintf(stderr, "Rf_initEmbeddedR: error creating Java VM, exiting...\n");
		return 1;
	}

	rInterfaceCallbacksClass = checkFindClass(jniEnv, "com/oracle/truffle/r/runtime/RInterfaceCallbacks");
	rembeddedClass = checkFindClass(jniEnv, "com/oracle/truffle/r/engine/shell/REmbedded");
	jclass stringClass = checkFindClass(jniEnv, "java/lang/String");
	jmethodID initializeMethod = checkGetMethodID(jniEnv, rembeddedClass, "initializeR", "([Ljava/lang/String;Z)V", 1);
	jobjectArray argsArray = (*jniEnv)->NewObjectArray(jniEnv, argc, stringClass, NULL);
	for (int i = 0; i < argc; i++) {
		jstring arg = (*jniEnv)->NewStringUTF(jniEnv, argv[i]);
		(*jniEnv)->SetObjectArrayElement(jniEnv, argsArray, i, arg);
	}
	if (setupRmainloop) {
        setupOverrides();
	}
	// Can't TRACE this upcall as system not initialized
	(*jniEnv)->CallStaticObjectMethod(jniEnv, rembeddedClass, initializeMethod, argsArray, setupRmainloop);
	initialized++;
	return 0;
}

// -----------------------------------------------------------------------------------------------
// -----------------------------------------------------------------------------------------------
// UpCalls from native to Java (IDE and tools up-calls)

char *R_HomeDir(void) {
    // TODO: why here? is it not implemented by RFFI already?
    return ((call_R_HomeDir) callbacks[R_HomeDir_x])();
}

CTXT R_getGlobalFunctionContext() {
    return ((call_R_getGlobalFunctionContext) callbacks[R_getGlobalFunctionContext_x])();
}

CTXT R_getParentFunctionContext(CTXT c) {
	return ((call_R_getParentFunctionContext) callbacks[R_getParentFunctionContext_x])(c);
}

SEXP R_getContextEnv(CTXT c) {
	return ((call_R_getContextEnv) callbacks[R_getContextEnv_x])(c);
}

SEXP R_getContextFun(CTXT c) {
	return ((call_R_getContextFun) callbacks[R_getContextFun_x])(c);
}

SEXP R_getContextCall(CTXT c) {
	return ((call_R_getContextCall) callbacks[R_getContextCall_x])(c);
}

SEXP R_getContextSrcRef(CTXT c) {
    return ((call_R_getContextSrcRef) callbacks[R_getContextSrcRef_x])(c);
}

int R_insideBrowser() {
    return ((call_R_insideBrowser) callbacks[R_insideBrowser_x])();
}

int R_isGlobal(CTXT c) {
    return ((call_R_isGlobal) callbacks[R_isGlobal_x])(c);
}

int R_isEqual(void* x, void* y) {
	return ((call_R_isEqual) callbacks[R_isEqual_x])(x, y);
}

// -----------------------------------------------------------------------------------------------
// -----------------------------------------------------------------------------------------------
// Downcalls from Java. We invoke these functions via REmbedRFFI

static void writeConsoleImpl(char *cbuf, int len, int otype) {
    if (ptr_R_WriteConsole == NULL) {
        // otype gives std (0) or err (1)
        (*ptr_R_WriteConsoleEx)(cbuf, len, otype);
    } else {
        (*ptr_R_WriteConsole)(cbuf, len);
    }
}

void rembedded_cleanup(int x, int y, int z) {
    ptr_R_CleanUp(x, y, z);
}

void rembedded_suicide(char* msg) {
    ptr_R_Suicide(msg);
}

void rembedded_write_console(char *cbuf, int len) {
    writeConsoleImpl(cbuf, len, 0);
}

void rembedded_write_err_console(char *cbuf, int len) {
    writeConsoleImpl(cbuf, len, 1);
}

char* rembedded_read_console(const char* prompt) {
    unsigned char* cbuf = malloc(sizeof(char) * 1024);
    int n = (*ptr_R_ReadConsole)(prompt, cbuf, 1024, 0);
    return cbuf;
}

// -----------------------------------------------------------------------------------------------
// -----------------------------------------------------------------------------------------------
// Unimplemented API functions (to make the linker happy and as a TODO list)

void R_SaveGlobalEnvToFile(const char *f) {
	unimplemented("R_SaveGlobalEnvToFile");
}

void uR_ShowMessage(const char *x) {
	unimplemented("R_ShowMessage");
}

int uR_ReadConsole(const char *a, unsigned char *b, int c, int d) {
	unimplemented("R_ReadConsole");
	return 0;
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

void R_SizeFromEnv(Rstart rs) {
	unimplemented("R_SizeFromEnv");
}

void R_common_command_line(int *a, char **b, Rstart rs) {
	unimplemented("R_common_command_line");
}

void R_set_command_line_arguments(int argc, char **argv) {
	unimplemented("R_set_command_line_arguments");
}

int uR_ShowFiles(int a, const char **b, const char **c,
	       const char *d, Rboolean e, const char *f) {
	unimplemented("R_ShowFiles");
	return 0;
}

int uR_ChooseFile(int a, char *b, int c) {
	unimplemented("R_ChooseFile");
	return 0;
}

int uR_EditFile(const char *a) {
	unimplemented("R_EditFile");
	return 0;
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

int uR_EditFiles(int a, const char **b, const char **c, const char *d) {
	unimplemented("uR_EditFiles");
	return 0;
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

void uR_PolledEvents(void) {
	unimplemented("R_PolledEvents");
}

void Rf_jump_to_toplevel() {
	unimplemented("Rf_jump_to_toplevel");
}

#include <R_ext/eventloop.h>

fd_set *R_checkActivity(int usec, int ignore_stdin) {
	unimplemented("R_checkActivity");
	return NULL;
}

void R_runHandlers(InputHandler *handlers, fd_set *mask) {
	unimplemented("R_runHandlers");
}

// -----------------------------------------------------------------------------------------------
// -----------------------------------------------------------------------------------------------
// Functions that can be assigned by an embedded client to change behavior.
// On the Java side the RInterfaceCallbacks remembers for each callback whether its default
// value (pointer to the default implementation) was changed for some user defined function
// and in such case Java down-calls to that function if the event occures. Moreover the default
// values of those pointers (and functions they point to) should be also functional (not all are yet),
// because the user code sometimes saves the original value before overriding it to invoke it in the
// user's overridden version. 'setupOverrides' updates the Java side enum via JNI and is invoked from
// public functions that initialize the R embedding at the point where those pointers should be
// already overridden by the user. (We may reconsider that and always down-call if some embedding
// applications override those pointers after initialization).


// Note: pointer to this function is typically saved by the user to be called from that
// user's R_Suicide override to actually really commit the suicide. We invoke this through
// JNI intentionally to avoid any potential problems with NFI being called while destroying the VM.
void uR_Suicide(const char *x) {
	JNIEnv *jniEnv = getEnv();
	jstring msg = (*jniEnv)->NewStringUTF(jniEnv, x);
	jmethodID suicideMethod = checkGetMethodID(jniEnv, rembeddedClass, "R_Suicide", "(Ljava/lang/String;)V", 1);
	(*jniEnv)->CallStaticVoidMethod(jniEnv, rembeddedClass, suicideMethod, msg);
}

void uR_CleanUp(SA_TYPE x, int y, int z) {
    ((call_R_CleanUp) callbacks[R_CleanUp_x])(x, y, z);
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
void (* R_PolledEvents)(void) = uR_PolledEvents;

// This call cannot be made via callbacks array because it may be invoked before FastR is fully initialized.
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

// -----------------------------------------------------------------------------------------------
// -----------------------------------------------------------------------------------------------
// Helpers

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

#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>
#include <errno.h>

static void perror_exit(char *msg) {
	perror(msg);
	exit(1);
}

static void *dlopen_jvmlib(char *libpath) {
	void *handle = dlopen(libpath, RTLD_GLOBAL | RTLD_NOW);
	if (handle == NULL) {
		fprintf(stderr, "Rf_initialize_R: cannot dlopen %s: %s\n", libpath, dlerror());
		exit(1);
	}
	return handle;
}

static JNIEnv* getEnv() {
    return jniEnv;
}

static jmethodID checkGetMethodID(JNIEnv *env, jclass klass, const char *name, const char *sig, int isStatic) {
    jmethodID methodID = isStatic ? (*env)->GetStaticMethodID(env, klass, name, sig) : (*env)->GetMethodID(env, klass, name, sig);
    if (methodID == NULL) {
        char buf[1024];
        strcpy(buf, "failed to find ");
        strcat(buf, isStatic ? "static" : "instance");
        strcat(buf, " method ");
        strcat(buf, name);
        strcat(buf, "(");
        strcat(buf, sig);
        strcat(buf, ")");
        (*env)->FatalError(env, buf);
    }
    return methodID;
}

static jclass checkFindClass(JNIEnv *env, const char *name) {
    jclass klass = (*env)->FindClass(env, name);
    if (klass == NULL) {
        char buf[1024];
        strcpy(buf, "failed to find class ");
        strcat(buf, name);
        strcat(buf, ".\nDid you set R_HOME to the correct location?");
        (*env)->FatalError(env, buf);
    }
    return (*env)->NewGlobalRef(env, klass);
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

// debugging
static void print_environ(char **env) {
	fprintf(stdout, "## Environment variables at %p\n", env);
	char **e = env;
	while (*e != NULL) {
		fprintf(stdout, "%s\n", *e);
		e++;
	}
}
