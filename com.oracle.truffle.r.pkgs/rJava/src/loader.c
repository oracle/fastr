/* functions dealing with the rJava class loader
 *
 * rJava R/Java interface  (C)Copyright 2003-2007 Simon Urbanek
 * (see rJava project root for licensing details)
 */

#include "rJava.h"

jclass   clClassLoader = (jclass) 0;
jobject  oClassLoader = (jobject) 0;

HIDE int initClassLoader(JNIEnv *env, jobject cl) {
  clClassLoader = (*env)->NewGlobalRef(env, (*env)->GetObjectClass(env, cl));
  /* oClassLoader = (*env)->NewGlobalRef(env, cl); */ 
  oClassLoader = cl;
#ifdef DEBUG_CL
  printf("initClassLoader: cl=%x, clCl=%x, jcl=%x\n", oClassLoader, clClassLoader, javaClassClass);
#endif
  return 0;
}

REPC SEXP RJava_set_class_loader(SEXP ldr) {
  JNIEnv *env=getJNIEnv();
  if (TYPEOF(ldr) != EXTPTRSXP)
    error("invalid type");
  if (!env)
    error("VM not initialized");
  
  jverify(ldr);
  initClassLoader(env, (jobject)EXTPTR_PTR(ldr));
  return R_NilValue;
}

REPC SEXP RJava_primary_class_loader() {
  JNIEnv *env=getJNIEnv();
  jclass cl = (*env)->FindClass(env, "RJavaClassLoader");
  _dbg(Rprintf("RJava_primary_class_loader, cl = %x\n", (int) cl));
  if (cl) {
    jmethodID mid = (*env)->GetStaticMethodID(env, cl, "getPrimaryLoader", "()LRJavaClassLoader;");
    _dbg(Rprintf(" - mid = %d\n", (int) mid));
    if (mid) {
      jobject o = (*env)->CallStaticObjectMethod(env, cl, mid);
      _dbg(Rprintf(" - call result = %x\n", (int) o));
      if (o) {
	return j2SEXP(env, o, 1);
      }
    }
  }
  checkExceptionsX(env, 1);

#ifdef NEW123
  jclass cl = (*env)->FindClass(env, "JRIBootstrap");
  Rprintf("RJava_primary_class_loader, cl = %x\n", (int) cl);
  if (cl) {
    jmethodID mid = (*env)->GetStaticMethodID(env, cl, "getBootRJavaLoader", "()Ljava/lang/Object;");
    Rprintf(" - mid = %d\n", (int) mid);
    if (mid) {
      jobject o = (*env)->CallStaticObjectMethod(env, cl, mid);
      Rprintf(" - call result = %x\n", (int) o);
      if (o) {
	return j2SEXP(env, o, 1);
      }
    }
  }
  checkExceptionsX(env, 1);
#endif
  return R_NilValue; 
}

REPC SEXP RJava_new_class_loader(SEXP p1, SEXP p2) {
  JNIEnv *env=getJNIEnv();
  
  const char *c1 = CHAR(STRING_ELT(p1, 0));
  const char *c2 = CHAR(STRING_ELT(p2, 0));
  jstring s1 = newString(env, c1);
  jstring s2 = newString(env, c2);

  jclass cl = (*env)->FindClass(env, "RJavaClassLoader");
  _dbg(Rprintf("find rJavaClassLoader: %x\n", (int) cl));
  jmethodID mid = (*env)->GetMethodID(env, cl, "<init>", "(Ljava/lang/String;Ljava/lang/String;)V");
  _dbg(Rprintf("constructor mid: %x\n", mid));
  jobject o = (*env)->NewObject(env, cl, mid, s1, s2);
  _dbg(Rprintf("new object: %x\n", o));
  o = makeGlobal(env, o);
  _dbg(Rprintf("calling initClassLoader\n"));
  initClassLoader(env, o);
  releaseObject(env, s1);
  releaseObject(env, s2);
  releaseObject(env, cl);
  return R_NilValue;
}
