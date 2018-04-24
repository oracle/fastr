/* misc. utility functions, mostly callable from R
 *
 * rJava R/Java interface  (C)Copyright 2003-2007 Simon Urbanek
 * (see rJava project root for licensing details)
 */

#include "rJava.h"

/** jobjRefInt object : string */
REPE SEXP RgetStringValue(SEXP par) {
  SEXP p,e,r;
  jstring s;
  const char *c;
  JNIEnv *env=getJNIEnv();
  
  profStart();
  p=CDR(par); e=CAR(p); p=CDR(p);
  if (e==R_NilValue) return R_NilValue;
  if (TYPEOF(e)==EXTPTRSXP) {
    jverify(e);
    s = (jstring)EXTPTR_PTR(e);
  } else {
    error("invalid object parameter");
    s = 0;
  }
  if (!s) return R_NilValue;
  c=(*env)->GetStringUTFChars(env, s, 0);
  if (!c)
    error("cannot retrieve string content");
  r = mkString(c);
  (*env)->ReleaseStringUTFChars(env, s, c);
  _prof(profReport("RgetStringValue:"));
  return r;
}

/** calls .toString() of the object and returns the corresponding string java object */
HIDE jstring callToString(JNIEnv *env, jobject o) {
  jclass cls;
  jmethodID mid;
  jstring s;

  if (!o) { _dbg(rjprintf("callToString: invoked on a NULL object\n")); return 0; }
  cls=objectClass(env,o);
  if (!cls) {
    _dbg(rjprintf("callToString: can't determine class of the object\n"));
    releaseObject(env, cls);
    checkExceptionsX(env, 1);
    return 0;
  }
  mid=(*env)->GetMethodID(env, cls, "toString", "()Ljava/lang/String;");
  if (!mid) {
    _dbg(rjprintf("callToString: toString not found for the object\n"));
    releaseObject(env, cls);
    checkExceptionsX(env, 1);
    return 0;
  }
  BEGIN_RJAVA_CALL;
  s = (jstring)(*env)->CallObjectMethod(env, o, mid);
  END_RJAVA_CALL;
  _mp(MEM_PROF_OUT("  %08x LNEW object method toString result\n", (int) s))
  releaseObject(env, cls);
  return s;
}

/** calls .toString() on the passed object (int/extptr) and returns the string 
    value or NULL if there is no toString method */
REPE SEXP RtoString(SEXP par) {
  SEXP p,e,r;
  jstring s;
  jobject o;
  const char *c;
  JNIEnv *env=getJNIEnv();

  p=CDR(par); e=CAR(p); p=CDR(p);
  if (e==R_NilValue) return R_NilValue;
  if (TYPEOF(e)==EXTPTRSXP) {
    jverify(e);
    o=(jobject)EXTPTR_PTR(e);
  } else
    error_return("RtoString: invalid object parameter");
  if (!o) return R_NilValue;
  s=callToString(env, o);
  if (!s) {
    return R_NilValue;
  }
  c=(*env)->GetStringUTFChars(env, s, 0);
  PROTECT(r=allocVector(STRSXP,1));
  SET_STRING_ELT(r, 0, mkCharUTF8(c));
  UNPROTECT(1);
  (*env)->ReleaseStringUTFChars(env, s, c);
  releaseObject(env, s);
  return r;
}

/* compares two references */
REPC SEXP RidenticalRef(SEXP ref1, SEXP ref2) {
  SEXP r;
  if (TYPEOF(ref1)!=EXTPTRSXP || TYPEOF(ref2)!=EXTPTRSXP) return R_NilValue;
  jverify(ref1);
  jverify(ref2);
  r=allocVector(LGLSXP,1);
  LOGICAL(r)[0]=(R_ExternalPtrAddr(ref1)==R_ExternalPtrAddr(ref2));
  return r;
}

/** create a NULL external reference */
REPC SEXP RgetNullReference() {
  return R_MakeExternalPtr(0, R_NilValue, R_NilValue);
}

/** TRUE if cl1 x; cl2 y = (cl2) x ... is valid */
REPC SEXP RisAssignableFrom(SEXP cl1, SEXP cl2) {
  SEXP r;
  JNIEnv *env=getJNIEnv();

  if (TYPEOF(cl1)!=EXTPTRSXP || TYPEOF(cl2)!=EXTPTRSXP)
    error("invalid type");
  if (!env)
    error("VM not initialized");
  jverify(cl1);
  jverify(cl2);
  r=allocVector(LGLSXP,1);
  LOGICAL(r)[0]=((*env)->IsAssignableFrom(env,
					  (jclass)EXTPTR_PTR(cl1),
					  (jclass)EXTPTR_PTR(cl2)));
  return r;
}

REPC SEXP RJava_checkJVM() {
  SEXP r = allocVector(LGLSXP, 1);
  LOGICAL(r)[0] = 0;
  if (!jvm || !getJNIEnv()) return r;
  LOGICAL(r)[0] = 1;
  return r;
}

extern int rJava_initialized; /* in callJNI.c */

REPC SEXP RJava_needs_init() {
  SEXP r = allocVector(LGLSXP, 1);
  LOGICAL(r)[0] = rJava_initialized?0:1;
  return r;
}

REPC SEXP RJava_set_memprof(SEXP fn) {
#ifdef MEMPROF
  const char *cFn = CHAR(STRING_ELT(fn, 0));
  int env = 0; /* we're just faking it so we can call MEM_PROF_OUT */
  
  if (memprof_f) fclose(memprof_f);
  if (cFn && !cFn[0]) {
    memprof_f = 0; return R_NilValue;
  }
  if (!cFn || (cFn[0]=='-' && !cFn[1]))
    memprof_f = stdout;
  else
    memprof_f = fopen(cFn, "a");
  if (!memprof_f) error("Cannot create memory profile file.");
  MEM_PROF_OUT("  00000000 REST -- profiling file set --\n");
  return R_NilValue;
#else
  error("Memory profiling support was not enabled in rJava.");
#endif
  return R_NilValue;
}
