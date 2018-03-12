#include <R.h>
#include <Rversion.h>
#include <Rdefines.h>
#include "rJava.h"
#include <stdlib.h>
#include <string.h>

/* determine whether eenv chache should be used (has no effect if JNI_CACHE is not set) */
int use_eenv = 1;

/* cached environment. Do NOT use directly! Always use getJNIEnv()! */
JNIEnv *eenv;

/* -- hack to get at the current call from C code using contexts */
#if ( R_VERSION >= R_Version(1, 7, 0) )
#include <setjmp.h>

/* stuff we need to pull for Windows... */
#ifdef WIN32
/* this is from gnuwin32/fixed/h/psignal.h */
#ifndef _SIGSET_T_
#define _SIGSET_T_
typedef int sigset_t;
#endif  /* Not _SIGSET_T_ */
typedef struct
{    
  jmp_buf jmpbuf;     /* Calling environment.  */  
  int mask_was_saved;       /* Saved the signal mask?  */                   
  sigset_t saved_mask;      /* Saved signal mask.  */                       
} sigjmp_buf[1];
/* we need to set HAVE_POSIX_SETJMP since we don't have config.h on Win */
#ifndef HAVE_POSIX_SETJMP
#define HAVE_POSIX_SETJMP
#endif
#endif

#ifdef HAVE_POSIX_SETJMP
#define JMP_BUF sigjmp_buf
#else
#define JMP_BUF jmp_buf
#endif

#ifndef CTXT_BUILTIN
#define CTXT_BUILTIN 64
#endif

typedef struct RCNTXT { /* this RCNTXT structure is only partial since we need to get at "call" - it is safe form R 1.7.0 on */
	struct RCNTXT *nextcontext; /* The next context up the chain <<-- we use this one to skip the .Call/.External call frame */
	int callflag;               /* The context "type" <<<-- we use this one to skip the .Call/.External call frame */
	JMP_BUF cjmpbuf;            /* C stack and register information */
	int cstacktop;              /* Top of the pointer protection stack */
	int evaldepth;              /* evaluation depth at inception */
	SEXP promargs;              /* Promises supplied to closure */
	SEXP callfun;               /* The closure called */
	SEXP sysparent;             /* environment the closure was called from */
	SEXP call;                  /* The call that effected this context <<<--- we pass this one to the condition */
	SEXP cloenv;                /* The environment */
} RCNTXT;

#ifndef LibExtern
#define LibExtern extern
#endif

LibExtern RCNTXT* R_GlobalContext;

static SEXP getCurrentCall() {
	RCNTXT *ctx = R_GlobalContext;
	/* skip the .External/.Call context to get at the underlying call */
	if (ctx->nextcontext && (ctx->callflag & CTXT_BUILTIN))
		ctx = ctx->nextcontext;
	/* skip .jcheck */
	if (TYPEOF(ctx->call) == LANGSXP && CAR(ctx->call) == install(".jcheck") && ctx->nextcontext)
		ctx = ctx->nextcontext;		
	return ctx->call;
}
#else
static SEXP getCurrentCall() {
	return R_NilValue;
}
#endif
/* -- end of hack */

/** throw an exception using R condition code.
 *  @param msg - message string
 *  @param jobj - jobjRef object of the exception 
 *  @param clazzes - simple name of all the classes in the inheritance tree of the exception plus "error" and "condition"
 */
HIDE void throwR(SEXP msg, SEXP jobj, SEXP clazzes) {
	SEXP cond = PROTECT(allocVector(VECSXP, 3));
	SEXP names = PROTECT(allocVector(STRSXP, 3));
	SET_VECTOR_ELT(cond, 0, msg);
	SET_VECTOR_ELT(cond, 1, getCurrentCall());
	SET_VECTOR_ELT(cond, 2, jobj);
	SET_STRING_ELT(names, 0, mkChar("message"));
	SET_STRING_ELT(names, 1, mkChar("call"));
	SET_STRING_ELT(names, 2, mkChar("jobj"));
	
	setAttrib(cond, R_NamesSymbol, names);
	setAttrib(cond, R_ClassSymbol, clazzes);
	UNPROTECT(2); /* clazzes, names */
	eval(LCONS(install("stop"), CONS(cond, R_NilValue)), R_GlobalEnv);
	UNPROTECT(1); /* cond */
}

/* check for exceptions and throw them to R level */
HIDE void ckx(JNIEnv *env) {
	SEXP xr, xobj, msg = 0, xclass = 0; /* note: we don't bother counting protections becasue we never return */
	jthrowable x = 0;
	if (env && !(x = (*env)->ExceptionOccurred(env))) return;
	if (!env) {
		env = getJNIEnv();
		if (!env)
			error("Unable to retrieve JVM environment.");
		ckx(env);
		return;
	}
	/* env is valid and an exception occurred */
	/* we create the jobj first, because the exception may in theory disappear after being cleared, 
	   yet this can be (also in theory) risky as it uses further JNI calls ... */
	xobj = j2SEXP(env, x, 0);
	(*env)->ExceptionClear(env);
	
	/* grab the list of class names (without package path) */
	SEXP clazzes = PROTECT( getSimpleClassNames_asSEXP( (jobject)x, (jboolean)1 ) ) ;
	
	/* ok, now this is a critical part that we do manually to avoid recursion */
	{
		jclass cls = (*env)->GetObjectClass(env, x);
		if (cls) {
			jstring cname;
			jmethodID mid = (*env)->GetMethodID(env, cls, "toString", "()Ljava/lang/String;");
			if (mid) {
				jstring s = (jstring)(*env)->CallObjectMethod(env, x, mid);
				if (s) {
					const char *c = (*env)->GetStringUTFChars(env, s, 0);
					if (c) {
						msg = PROTECT(mkString(c));
						(*env)->ReleaseStringUTFChars(env, s, c);
					}
				}
			}
			/* beside toString() we also need to call getName() on cls to get the subclass */
			cname = (jstring) (*env)->CallObjectMethod(env, cls, mid_getName);
			if (cname) {
				const char *c = (*env)->GetStringUTFChars(env, cname, 0);
				if (c) {                          
					/* convert full class name to JNI notation */
					char *cn = strdup(c), *d = cn;
					while (*d) { if (*d == '.') *d = '/'; d++; }
					xclass = mkString(cn);
					free(cn);
					(*env)->ReleaseStringUTFChars(env, cname, c);
				}		
				(*env)->DeleteLocalRef(env, cname);
			}
			if ((*env)->ExceptionOccurred(env))
				(*env)->ExceptionClear(env);
			(*env)->DeleteLocalRef(env, cls);
		} else (*env)->ExceptionClear(env);
		if (!msg)
			msg = PROTECT(mkString("Java Exception <no description because toString() failed>"));
	}
	/* delete the local reference to the exception (jobjRef has a global copy) */
	(*env)->DeleteLocalRef(env, x);

	/* construct the jobjRef */
	xr = PROTECT(NEW_OBJECT(MAKE_CLASS("jobjRef")));
	if (inherits(xr, "jobjRef")) {
		SET_SLOT(xr, install("jclass"), xclass ? xclass : mkString("java/lang/Throwable"));
		SET_SLOT(xr, install("jobj"), xobj);
	}
	
	/* and off to R .. (we're keeping xr and clazzes protected) */
	throwR(msg, xr, clazzes);
	/* throwR never returns so don't even bother ... */
}

/* clear any pending exceptions */
HIDE void clx(JNIEnv *env) {
	if (env && (*env)->ExceptionOccurred(env))
		(*env)->ExceptionClear(env);
}

#ifdef JNI_CACHE
HIDE JNIEnv *getJNIEnvSafe();
HIDE JNIEnv *getJNIEnv() {
  return (use_eenv)?eenv:getJNIEnvSafe();
}

HIDE JNIEnv *getJNIEnvSafe()
#else
HIDE JNIEnv *getJNIEnv()
#endif
  {
    JNIEnv *env;
    jsize l;
    jint res;

    if (!jvm) { /* we're hoping that the JVM pointer won't change :P we fetch it just once */
        res = JNI_GetCreatedJavaVMs(&jvm, 1, &l);
        if (res != 0) {
            error("JNI_GetCreatedJavaVMs failed! (result:%d)",(int)res); return 0;
        }
        if (l < 1)
	    error("No running JVM detected. Maybe .jinit() would help.");
	if (!rJava_initialized)
	    error("rJava was called from a running JVM without .jinit().");
    }
    res = (*jvm)->AttachCurrentThread(jvm, (void**) &env, 0);
    if (res!=0) {
      error("AttachCurrentThread failed! (result:%d)", (int)res); return 0;
    }
    if (env && !eenv) eenv=env;
    
    /* if (eenv!=env)
        fprintf(stderr, "Warning! eenv=%x, but env=%x - different environments encountered!\n", eenv, env); */
    return env;
}

REP void RuseJNICache(int *flag) {
  if (flag) use_eenv=*flag;
}
