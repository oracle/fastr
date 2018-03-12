#include "rJava.h"
#include <stdlib.h>
#include <string.h>
#include <stdio.h>

/* global variables */
JavaVM *jvm;

/* this will be set when Java tries to exit() but we carry on */
int java_is_dead = 0;

/* cached, global objects */

jclass javaStringClass;
jclass javaObjectClass;
jclass javaClassClass;
jclass javaFieldClass;

/* cached, global method IDs */
jmethodID mid_forName;
jmethodID mid_getName;
jmethodID mid_getSuperclass;
jmethodID mid_getType;
jmethodID mid_getField;

/* internal classes and methods */
jclass rj_RJavaTools_Class = (jclass)0;
jmethodID mid_rj_getSimpleClassNames = (jmethodID)0 ;
jmethodID mid_RJavaTools_getFieldTypeName = (jmethodID)0 ;


jclass rj_RJavaImport_Class = (jclass)0;
jmethodID mid_RJavaImport_getKnownClasses = (jmethodID)0 ;
jmethodID mid_RJavaImport_lookup = (jmethodID)0 ;
jmethodID mid_RJavaImport_exists = (jmethodID)0 ;

int rJava_initialized = 0;


static int    jvm_opts=0;
static char **jvm_optv=0;

#ifdef JNI_VERSION_1_2 
static JavaVMOption *vm_options;
static JavaVMInitArgs vm_args;
#else  
#error "Java/JNI 1.2 or higher is required!"
#endif

#define H_OUT  1
#define H_EXIT 2

#ifdef Win32
/* the hooks are reportedly causing problems on Windows, so disable them by default */
static int default_hooks = 0;
#else
static int default_hooks = H_OUT|H_EXIT;
#endif

static int JNICALL vfprintf_hook(FILE *f, const char *fmt, va_list ap) {
#ifdef Win32
  /* on Windows f doesn't refer to neither stderr nor stdout,
     so we have no way of finding out the target, so we assume stderr */
  REvprintf(fmt, ap);
  return 0;
#else
  if (f==stderr) {
    REvprintf(fmt, ap);
    return 0;
  } else if (f==stdout) {
    Rvprintf(fmt, ap);
    return 0;
  }
  return vfprintf(f, fmt, ap);
#endif
}

static void JNICALL exit_hook(int status) {
    /* REprintf("\nJava requested System.exit(%d), trying to raise R error - this may crash if Java is in a bad state.\n", status); */
    java_is_dead = 1;
    Rf_error("Java called System.exit(%d) requesting R to quit - trying to recover", status);
    /* FIXME: we could do something smart here such as running a call-back
       into R ... jump into R event loop ... at any rate we cannot return,
       but we don't want to kill R ... */
    exit(status);
}

/* in reality WIN64 implies WIN32 but to make sure ... */
#if defined(_WIN64) || defined(_WIN32)
#include <io.h>
#include <fcntl.h>
#endif

static int initJVM(const char *user_classpath, int opts, char **optv, int hooks) {
  int total_num_properties, propNum = 0;
  jint res;
  char *classpath;
  
  if(!user_classpath)
    /* use the CLASSPATH environment variable as default */
    user_classpath = getenv("CLASSPATH");
  if(!user_classpath) user_classpath = "";
  
  vm_args.version = JNI_VERSION_1_2;
  if(JNI_GetDefaultJavaVMInitArgs(&vm_args) != JNI_OK) {
    error("JNI 1.2 or higher is required");
    return -1;    
  }
    
  /* leave room for class.path, and optional jni args */
  total_num_properties = 6 + opts;
    
  vm_options = (JavaVMOption *) calloc(total_num_properties, sizeof(JavaVMOption));
  vm_args.version = JNI_VERSION_1_2; /* should we do that or keep the default? */
  vm_args.options = vm_options;
  vm_args.ignoreUnrecognized = JNI_TRUE;
  
  classpath = (char*) calloc(24 + strlen(user_classpath), sizeof(char));
  sprintf(classpath, "-Djava.class.path=%s", user_classpath);
  
  vm_options[propNum++].optionString = classpath;   
  
  /*   print JNI-related messages */
  /* vm_options[propNum++].optionString = "-verbose:class,jni"; */
  
  if (optv) {
    int i=0;
    while (i<opts) {
      if (optv[i]) vm_options[propNum++].optionString = optv[i];
      i++;
    }
  }
  if (hooks&H_OUT) {
    vm_options[propNum].optionString = "vfprintf";
    vm_options[propNum++].extraInfo  = vfprintf_hook;
  }
  if (hooks&H_EXIT) {
    vm_options[propNum].optionString = "exit";
    vm_options[propNum++].extraInfo  = exit_hook;
  }
  vm_args.nOptions = propNum;

  /* Create the Java VM */
  res = JNI_CreateJavaVM(&jvm,(void **)&eenv, &vm_args);
  
  if (res != 0)
    error("Cannot create Java virtual machine (%d)", res);
  if (!eenv)
    error("Cannot obtain JVM environemnt");

#if defined(_WIN64) || defined(_WIN32)
  _setmode(0, _O_TEXT);
#endif

  return 0;
}

#ifdef THREADS
#include <pthread.h>

#ifdef XXDARWIN
#include <Carbon/Carbon.h>
#include <CoreFoundation/CoreFoundation.h>
#include <CoreFoundation/CFRunLoop.h>
#endif

pthread_t initJVMpt;
pthread_mutex_t initMutex = PTHREAD_MUTEX_INITIALIZER;

int thInitResult = 0;
int initAWT = 0;

static void *initJVMthread(void *classpath)
{
  int ws;
  jclass c;
  JNIEnv *lenv;

  thInitResult=initJVM((char*)classpath, jvm_opts, jvm_optv, default_hooks);
  if (thInitResult) return 0;

  init_rJava();

  lenv = eenv; /* we make a local copy before unlocking just in case
		  someone messes with it before we can use it */

  _dbg(rjprintf("initJVMthread: unlocking\n"));
  pthread_mutex_unlock(&initMutex);

  if (initAWT) {
    _dbg(rjprintf("initJVMthread: get AWT class\n"));
    /* we are still on the same thread, so we can safely use eenv */
    c = (*lenv)->FindClass(lenv, "java/awt/Frame");
  }

  _dbg(rjprintf("initJVMthread: returning from the init thread.\n"));
  return 0;
}

#endif



/* initialize internal structures/variables of rJava.
   The JVM initialization was performed before (but may have failed)
*/
HIDE void init_rJava(void) {
  jclass c;
  JNIEnv *env = getJNIEnv();
  if (!env) return; /* initJVMfailed, so we cannot proceed */
  
  /* get global classes. we make the references explicitely global (although unloading of String/Object is more than unlikely) */
  c=(*env)->FindClass(env, "java/lang/String");
  if (!c) error("unable to find the basic String class");
  javaStringClass=(*env)->NewGlobalRef(env, c);
  if (!javaStringClass) error("unable to create a global reference to the basic String class");
  (*env)->DeleteLocalRef(env, c);

  c=(*env)->FindClass(env, "java/lang/Object");
  if (!c) error("unable to find the basic Object class");
  javaObjectClass=(*env)->NewGlobalRef(env, c);
  if (!javaObjectClass) error("unable to create a global reference to the basic Object class");
  (*env)->DeleteLocalRef(env, c);

  c = (*env)->FindClass(env, "java/lang/Class");
  if (!c) error("unable to find the basic Class class");
  javaClassClass=(*env)->NewGlobalRef(env, c);
  if (!javaClassClass) error("unable to create a global reference to the basic Class class");
  (*env)->DeleteLocalRef(env, c);

  c = (*env)->FindClass(env, "java/lang/reflect/Field");
  if (!c) error("unable to find the basic Field class");
  javaFieldClass=(*env)->NewGlobalRef(env, c);
  if (!javaFieldClass) error("unable to create a global reference to the basic Class class");
  (*env)->DeleteLocalRef(env, c);

  mid_forName  = (*env)->GetStaticMethodID(env, javaClassClass, "forName", "(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;");
  if (!mid_forName) error("cannot obtain Class.forName method ID");
  
  mid_getName  = (*env)->GetMethodID(env, javaClassClass, "getName", "()Ljava/lang/String;");
  if (!mid_getName) error("cannot obtain Class.getName method ID");
  
  mid_getSuperclass =(*env)->GetMethodID(env, javaClassClass, "getSuperclass", "()Ljava/lang/Class;");
  if (!mid_getSuperclass) error("cannot obtain Class.getSuperclass method ID");
  
  mid_getField = (*env)->GetMethodID(env, javaClassClass, "getField",
				     "(Ljava/lang/String;)Ljava/lang/reflect/Field;");
  if (!mid_getField) error("cannot obtain Class.getField method ID");
 
  mid_getType  = (*env)->GetMethodID(env, javaFieldClass, "getType",
				     "()Ljava/lang/Class;");
  if (!mid_getType) error("cannot obtain Field.getType method ID");

  rJava_initialized = 1;
}


static SEXP RinitJVM_real(SEXP par)
{
  const char *c=0;
  SEXP e=CADR(par);
  int r=0;
  JavaVM *jvms[32];
  jsize vms=0;
  
  jvm_opts=0;
  jvm_optv=0;
  
  if (TYPEOF(e)==STRSXP && LENGTH(e)>0)
	  c=CHAR(STRING_ELT(e,0));

  e = CADDR(par);
  if (TYPEOF(e)==STRSXP && LENGTH(e)>0) {
      int len = LENGTH(e), add_xrs = 1, joi = 0;
      jvm_optv = (char**)malloc(sizeof(char*) * (len + 3));
      if (!jvm_optv) Rf_error("Cannot allocate options buffer - out of memory");
#ifdef USE_HEADLESS_INIT
      /* prepend headless so the user can still override it */
      jvm_optv[jvm_opts++] = "-Djava.awt.headless=true";
#endif
      while (joi < len) {
	  jvm_optv[jvm_opts] = strdup(CHAR(STRING_ELT(e, joi++)));
#ifdef HAVE_XRS
	  /* check if Xrs is already used */
	  if (!strcmp(jvm_optv[jvm_opts], "-Xrs"))
	      add_xrs = 0;
#endif
	  jvm_opts++;
      }
#ifdef HAVE_XRS
      if (add_xrs)
	  jvm_optv[jvm_opts++] = "-Xrs";
#endif
  } else {
#ifdef USE_HEADLESS_INIT
      jvm_optv = (char**)malloc(sizeof(char*) * 3);
      if (!jvm_optv) Rf_error("Cannot allocate options buffer - out of memory");
      jvm_optv[jvm_opts++] = "-Djava.awt.headless=true";
#endif
#ifdef HAVE_XRS
      if (!jvm_optv) jvm_optv = (char**)malloc(sizeof(char*) * 2);
      if (!jvm_optv) Rf_error("Cannot allocate options buffer - out of memory");
      jvm_optv[jvm_opts++] = "-Xrs";
#endif
  }
  if (jvm_opts)
      jvm_optv[jvm_opts] = 0;
  
  r=JNI_GetCreatedJavaVMs(jvms, 32, &vms);
  if (r) {
    Rf_error("JNI_GetCreatedJavaVMs returned %d\n", r);
  } else {
    if (vms>0) {
      int i=0;
      _dbg(rjprintf("RinitJVM: Total %d JVMs found. Trying to attach the current thread.\n", (int)vms));
      while (i<vms) {
	if (jvms[i]) {
	  if (!(*jvms[i])->AttachCurrentThread(jvms[i], (void**)&eenv, NULL)) {
            _dbg(rjprintf("RinitJVM: Attached to existing JVM #%d.\n", i+1));
	    break;
	  }
	}
	i++;
      }
      if (i==vms) Rf_error("Failed to attach to any existing JVM.");
      else {
	jvm = jvms[i];
	init_rJava();
      }
      PROTECT(e=allocVector(INTSXP,1));
      INTEGER(e)[0]=(i==vms)?-2:1;
      UNPROTECT(1);
      return e;
    }
  }

#ifdef THREADS
  if (getenv("R_GUI_APP_VERSION") || getenv("RJAVA_INIT_AWT"))
    initAWT=1;

  _dbg(rjprintf("RinitJVM(threads): launching thread\n"));
  pthread_mutex_lock(&initMutex);
  pthread_create(&initJVMpt, 0, initJVMthread, c);
  _dbg(rjprintf("RinitJVM(threads): waiting for mutex\n"));
  pthread_mutex_lock(&initMutex);
  pthread_mutex_unlock(&initMutex);
  /* pthread_join(initJVMpt, 0); */
  _dbg(rjprintf("RinitJVM(threads): attach\n"));
  /* since JVM was initialized by another thread, we need to attach ourselves */
  (*jvm)->AttachCurrentThread(jvm, (void**)&eenv, NULL);
  _dbg(rjprintf("RinitJVM(threads): done.\n"));
  r = thInitResult;
#else
  profStart();
  r=initJVM(c, jvm_opts, jvm_optv, default_hooks);
  init_rJava();
  _prof(profReport("init_rJava:"));
  _dbg(rjprintf("RinitJVM(non-threaded): initJVM returned %d\n", r));
#endif
  if (jvm_optv) free(jvm_optv);
  jvm_opts=0;
  PROTECT(e=allocVector(INTSXP,1));
  INTEGER(e)[0]=r;
  UNPROTECT(1);
  return e;
}

/* This is a workaround for another workaround in Oracle JVM.  On Linux, the
   JVM would insert protected guard pages into the C stack of the R process
   that initializes the JVM, thus effectively reducing the stack size.
   Worse yet, R does not find out about this and fails to detect infinite
   recursion. Without the workaround, this code crashes R after .jinit() is called

   x <- 1; f <- function() { x <<- x + 1 ; print(x) ; f() } ; tryCatch(f(), error=function(e) x)

   and various packages fail/segfault unpredictably as they reach the stack
   limit.

   This workaround in rJava can detect the reduction of the R stack and can
   adjust R_CStack* variables so that the detection of infinite recursion
   still works.  Moreover, the workaround in rJava can prevent significant
   shrinking of the stack by allocating at least 2M from the C stack before
   initializing the JVM.  This makes the JVM think that the current thread
   is actually not the initial thread (os::Linux::is_initial_thread()
   returns false), because is_initial_thread will run outside what the JVM
   assumes is the stack for the initial thread (the JVM caps the stack to
   2M).  Consequently, the JVM will not install guard pages.  The stack size
   limit is implemented in os::Linux::capture_initial_stack (the limit 4M on
   Itanium which is ignored here by rJava and 2M on other Linux systems) and
   is claimed to be a workaround for issues in RH7.2.  The problem is still
   present in JVM 9.  Moreover, on Linux the JVM inserts guard pages also
   based on the setting of -Xss.
*/

#undef JVM_STACK_WORKAROUND
#if defined(linux) || defined(__linux__) || defined(__linux)
#define JVM_STACK_WORKAROUND
#endif

#ifdef JVM_STACK_WORKAROUND

#include <stdint.h>

 /* unfortunately this needs (write!) access to R internals */
extern uintptr_t R_CStackLimit;
extern uintptr_t R_CStackStart;
extern int R_CStackDir;

#include <sys/types.h>
#include <sys/time.h>
#include <sys/wait.h>
#include <unistd.h>
#include <errno.h>
#include <sys/resource.h>
#include <inttypes.h>

/*
  Find a new limit for the C stack, within the existing limit.  Looks for
  the first address in the stack area that is not accessible to the current
  process.  This code could easily be made to work on different Unix
  systems, not just on Linux, but it does not seem necessary at the moment.

  from is the first address to access
  limit is the first address not to access
  bound is the new first address not to access
  dir is 1 (stack grows up) or -1 (stack grows down, the usual)
    so, incrementing by dir one traverses from stack start, from the oldest
    things on the stace; note that in R_CStackDir, 1 means stack grows down

  returns NULL in case of error, limit when no new limit is found,
    a new limit when found

  returns NULL when from == limit
*/

volatile int globald = 0;
static char* findBound(char *from, char *limit, int dir)
{
  int pipefd[2];
  pid_t cpid;
    /* any positive size could be used, using page size is a heuristic */
  int psize = (int) sysconf(_SC_PAGESIZE);
  char buf[psize];

  if ((dir==-1 && from<=limit) || (dir == 1 && from>=limit))
    return NULL;

  if (pipe(pipefd) == -1)
    return NULL; /* error */

  cpid = fork();
  if (cpid == -1)
    return NULL; /* error */

  if (cpid == 0) {
    /* child process, reads from the pipe */
    close(pipefd[1]);

    while (read(pipefd[0], &buf, psize) > 0);
    _exit(0);
  } else {
    /* Parent process, writes data to the pipe looking for EFAULT error which
       indicates an inaccesible page. For performance, the reading is first
       done using a buffer (of the size of a page), but once EFAULT is reached,
       it is re-tried byte-by-byte for the last buffer not read successfully.
    */
    close(pipefd[0]);
    intmax_t diff = imaxabs(from-limit);
    int step = (diff < psize) ? diff : psize;
    int failed = 0;
    int reached_fault = 0;
    char *origfrom = from;

    /* start search at bigger granularity for performance */
    for(; (dir == -1) ? (from-step+1 >= limit) : (from+step-1 <= limit)
        ; from += dir * step)

      if (write(pipefd[1], (dir == -1) ? (from-step+1) : from, step) == -1) {
        if (errno == EFAULT)
          reached_fault = 1;
        else
          failed = 1;
        break;
      }

    /* finetune with step 1 */
    if (reached_fault && step > 1 && origfrom != from)
      for(from -= dir * step; from != limit ; from += dir)
        if (write(pipefd[1], from, 1) == -1) {
          if (errno != EFAULT)
            failed = 1;
          break;
        }
    close(pipefd[1]);
    wait(NULL);
    return failed ? NULL : from;
  }
}

/* Add certain amount of padding to the C stack before invoking RinitJVM.
   The recursion turned out more reliable in face of compiler optimizations
   than allocating large arrays on the C stack, both via definition and
   alloca. */
static SEXP RinitJVM_with_padding(SEXP par, intptr_t padding, char *last) {
  char dummy[1];
    /* reduce the risk that dummy will be optimized out */
  dummy[0] = (char) (uintptr_t) &dummy;
  padding -= (last - dummy) * R_CStackDir;
  if (padding <= 0)
    return RinitJVM_real(par);
  else
    return RinitJVM_with_padding(par, padding, dummy);
}

/* Run RinitJVM with the Java stack workaround */
static SEXP RinitJVM_jsw(SEXP par) {

  /* One can disable the workaround using environment variable
     RJAVA_JVM_STACK_WORKAROUND

     this support exists for diagnostics and as a way to disable the
     workaround without re-compiling rJava.  In normal cases it only makes
     sense to use the default value of 3.
   */

  #define JSW_DISABLED 0
  #define JSW_DETECT   1
  #define JSW_ADJUST   2
  #define JSW_PREVENT  3

  #define JSW_PADDING 2*1024*1024
  #define JSW_CHECK_BOUND 16*1024*1024

  /* 0 - disabled
     1 - detect guard pages
     2 - detect guard pages and adjust R stack size
     3 - prevent guard page creation, detect, and adjust */
  int val = JSW_PREVENT;
  char *vval = getenv("RJAVA_JVM_STACK_WORKAROUND");
  if (vval != NULL)
    val = atoi(vval);
  if (val < 0 || val > 3)
    error("Invalid value for RJAVA_JVM_STACK_WORKAROUND");

  _dbg(rjprintf("JSW workaround: (level %d)\n", val));

  if (val == JSW_DISABLED)
    return RinitJVM_real(par);

  /* Figure out the original stack limit */
  uintptr_t rlimsize = 0;

  struct rlimit rlim;
  if (getrlimit(RLIMIT_STACK, &rlim) == 0) {
    rlim_t lim = rlim.rlim_cur;
    if (lim != RLIM_INFINITY) {
      rlimsize = (uintptr_t)lim;
       _dbg(rjprintf("  RLIMIT_STACK (rlimsize) %lu\n",
                     (unsigned long) rlimsize));
    } else {
       _dbg(rjprintf("  RLIMIT_STACK unlimited\n"));
    }
  }

  if (rlimsize == 0 && R_CStackLimit != -1) {
    /* getrlimit should work on linux, so this should only happen
       when stack size is unlimited */
    rlimsize = (uintptr_t) (R_CStackLimit / 0.95);
    _dbg(rjprintf("  expanded R_CStackLimit (rlimsize) %lu\n",
                  (unsigned long) rlimsize));
  }

  if (rlimsize == 0 || rlimsize > JSW_CHECK_BOUND) {
    /* use a reasonable limit when looking for guard pages when
       stack size is unlimited or very large  */
    rlimsize = JSW_CHECK_BOUND;
    _dbg(rjprintf("  hardcoded rlimsize %lu\n",
                  (unsigned long) rlimsize));
  }

  _dbg(rjprintf("  R_CStackStart %p\n", R_CStackStart));
  _dbg(rjprintf("  R_CStackLimit %lu\n", (unsigned long)R_CStackLimit));
  char *maxBound = (char *)((intptr_t)R_CStackStart - (intptr_t)R_CStackDir*rlimsize);
  char *oldBound = findBound((char*)R_CStackStart - R_CStackDir, maxBound, -R_CStackDir);
  _dbg(rjprintf("  maxBound %p\n", maxBound));
  _dbg(rjprintf("  oldBound %p\n", oldBound));

  /* it is expected that newBound < maxBound, because not all of the "rlim"
     stack may be accessible even before JVM initialization, which can be e.g.
     because of an imprecise detection of the stack start */

  intptr_t padding = 0;
  if (val >= JSW_PREVENT) {
    int dummy;
    intptr_t usage = R_CStackDir * (R_CStackStart - (uintptr_t)&dummy);
    usage += JSW_PADDING + 512; /* 512 is a buffer for C recursive calls */
    if(R_CStackLimit == -1 || usage < ((intptr_t) R_CStackLimit))
      padding = JSW_PADDING;
  }

  char dummy[1];
    /* reduce the risk that dummy will be optimized out */
  dummy[0] = (char) (uintptr_t) &dummy;
  SEXP ans = PROTECT(RinitJVM_with_padding(par, padding, dummy));
  _dbg(rjprintf("JSW workaround (ctd): (level %d)\n", val));

  if (val >= JSW_DETECT) {

    char *newBound = findBound((char*)R_CStackStart - R_CStackDir, oldBound, -R_CStackDir);
    _dbg(rjprintf("  newBound %p\n", newBound));
    if (oldBound == newBound) {
      /* No guard pages inserted, keep the original stack size.
         This includes the case when the original stack size was
         unlimited. */
      UNPROTECT(1); /* ans */
      return ans;
    }

    intptr_t newb = (intptr_t)newBound;
    intptr_t lim = ((intptr_t)R_CStackStart - newb) * R_CStackDir;
    uintptr_t newlim = (uintptr_t) (lim * 0.95);

    uintptr_t oldlim = R_CStackLimit;
    if (val >= JSW_ADJUST) {
      R_CStackLimit = newlim;
      _dbg(rjprintf("  new R_CStackLimit %lu\n", (unsigned long)R_CStackLimit));
    }

    /* Only report when the loss is big. There may be some bytes
       lost because even with the original setting of R_CStackLimit before
       initializing the JVM, one may not be able to access all bytes of stack
       (e.g. because of imprecise detection of the stack start). */

    int bigloss = 0;
    if (oldlim == -1) {
      /* the message may be confusing when R_CStackLimit was set to -1
         because the original stack size was too large */
      REprintf("Rjava.init.warning: stack size reduced from unlimited to"
        " %u bytes after JVM initialization.\n", newlim);
      bigloss = 1;
    } else {
      unsigned lost = (unsigned) (oldlim - newlim);
      if (lost > oldlim * 0.01) {
        REprintf("Rjava.init.warning: lost %u bytes of stack after JVM"
          " initialization.\n", lost);
        bigloss = 1;
      }
    }

    if (bigloss) {
      if (val >= JSW_PREVENT && padding == 0)
        REprintf("Rjava.init.warning: re-try with increased"
          " stack size via ulimit -s to allow for a work-around.\n");
      if (val < JSW_ADJUST)
        REprintf("Rjava.init.warning: R may crash in recursive calls.\n");
    }
  }

  UNPROTECT(1); /* ans */
  return ans;
}

#endif /* JVM_STACK_WORKAROUND */

/** RinitJVM(classpath)
    initializes JVM with the specified class path */
REP SEXP RinitJVM(SEXP par) {

#ifndef JVM_STACK_WORKAROUND
  return RinitJVM_real(par);
#else
  return RinitJVM_jsw(par);
#endif
}

REP void doneJVM() {
  (*jvm)->DestroyJavaVM(jvm);
  jvm = 0;
  eenv = 0;
}

/**
 * Initializes the cached values of classes and methods used internally
 * These classes and methods are the ones that are in rJava (RJavaTools, ...)
 * not java standard classes (Object, Class)
 */ 
REPC SEXP initRJavaTools(){

	JNIEnv *env=getJNIEnv();
	jclass c; 
	
	/* classes */
	
	/* RJavaTools class */
	c = findClass(env, "RJavaTools", oClassLoader);
	if (!c) error("unable to find the RJavaTools class");
	rj_RJavaTools_Class=(*env)->NewGlobalRef(env, c);
	if (!rj_RJavaTools_Class) error("unable to create a global reference to the RJavaTools class");
	(*env)->DeleteLocalRef(env, c);
	
	/* RJavaImport */
	c = findClass(env, "RJavaImport", oClassLoader); 
	if (!c) error("unable to find the RJavaImport class");
	rj_RJavaImport_Class=(*env)->NewGlobalRef(env, c);
	if (!rj_RJavaImport_Class) error("unable to create a global reference to the RJavaImport class");
	(*env)->DeleteLocalRef(env, c);
	
	
	/* methods */
	
	mid_RJavaTools_getFieldTypeName  = (*env)->GetStaticMethodID(env, rj_RJavaTools_Class, 
		"getFieldTypeName", "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/String;");
	if (!mid_RJavaTools_getFieldTypeName) error("cannot obtain RJavaTools.getFieldTypeName method ID");
	
	mid_rj_getSimpleClassNames  = (*env)->GetStaticMethodID(env, rj_RJavaTools_Class, 
		"getSimpleClassNames", "(Ljava/lang/Object;Z)[Ljava/lang/String;");
	if (!mid_rj_getSimpleClassNames) error("cannot obtain RJavaTools.getDimpleClassNames method ID");
	
	mid_RJavaImport_getKnownClasses = (*env)->GetMethodID(env, rj_RJavaImport_Class, 
		"getKnownClasses", "()[Ljava/lang/String;");
	if (!mid_RJavaImport_getKnownClasses) error("cannot obtain RJavaImport.getKnownClasses method ID");
	
	mid_RJavaImport_lookup = (*env)->GetMethodID(env, rj_RJavaImport_Class, 
		"lookup", "(Ljava/lang/String;)Ljava/lang/Class;");
	if( !mid_RJavaImport_lookup) error("cannot obtain RJavaImport.lookup method ID");
	
	mid_RJavaImport_exists = (*env)->GetMethodID(env, rj_RJavaImport_Class, 
		"exists", "(Ljava/lang/String;)Z");
	if( ! mid_RJavaImport_exists ) error("cannot obtain RJavaImport.exists method ID");
	// maybe add RJavaArrayTools, ...
	
	return R_NilValue; 
}

