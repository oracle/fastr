#include <R.h>
#include <Rinternals.h>
#include "rJava.h"

/* creates a reference to an R object on the Java side 
   1) lock down the object in R
   2) call new Rengine(eng,robj) {or any other class such as REXPReference for REngine API}
 */
REPC SEXP PushToREXP(SEXP clname, SEXP eng, SEXP engCl, SEXP robj, SEXP doConv) {
  char sig[128];
  jvalue jpar[4];
  jobject o;
  int convert = (doConv == R_NilValue) ? -1 : asInteger(doConv);
  JNIEnv *env=getJNIEnv();
  const char *cName;
  
  if (!isString(clname) || LENGTH(clname)!=1) error("invalid class name");
  if (!isString(engCl) || LENGTH(engCl)!=1) error("invalid engine class name");
  if (TYPEOF(eng)!=EXTPTRSXP) error("invalid engine object");
  R_PreserveObject(robj);
  sig[127]=0;
  cName = CHAR(STRING_ELT(clname,0));
  jpar[0].l = (jobject)EXTPTR_PTR(eng);
  jpar[1].j = (jlong) robj;
  if (convert == -1)
    snprintf(sig,127,"(L%s;J)V",CHAR(STRING_ELT(engCl,0)));
  else {
    snprintf(sig,127,"(L%s;JZ)V",CHAR(STRING_ELT(engCl,0)));
    jpar[2].z = (jboolean) convert;
  }
  o = createObject(env, cName, sig, jpar, 1);
  if (!o) error("Unable to create Java object");
  return j2SEXP(env, o, 1);
  /* ok, some thoughts on mem mgmt - j2SEXP registers a finalizer. But I believe that is ok, because the pushed reference is useless until it is passed as an argument to some Java method. And then, it will have another reference which will prevent the Java side from being collected. The R-side reference may be gone, but that's ok, because it's the Java finalizer that needs to clean up the pushed R object and for that it doesn't need the proxy object at all. This is the reason why RReleaseREXP uses EXTPTR - all the Java finalizaer has to do is to call RReleaseREXP(self). For that it can create a fresh proxy object containing the REXP. But here comes he crux - this proxy cannot again create a reference - it must be plain pass-through, so this part needs to be verified.

Note: as of REngine API the references assume protected objects and use rniRelease to clean up, so RReleaseREXP won't be called and is not needed. That is good, because RReleaseREXP assumes JRI objects whereas REngine will create REXPReference (no xp there). However, if we ever change that REXPReference assumption we will be in trouble.
 */
}

/* this is pretty much hard-coded for now - it's picking "xp" attribute */
REPC SEXP RReleaseREXP(SEXP ptr) {
  jobject o;
  if (TYPEOF(ptr)==EXTPTRSXP) error("invalid object");
  o = (jobject)EXTPTR_PTR(ptr);
  {
    JNIEnv *env = getJNIEnv();
    jclass cls = (*env)->GetObjectClass(env, o);
    if (cls) {
      jfieldID fid=(*env)->GetFieldID(env,cls,"xp","J");
      if (fid) {
	jlong r = (*env)->GetLongField(env, o, fid);
	SEXP x = (SEXP) r;
	if (x) R_ReleaseObject(x);
      }
    }
  }
  return R_NilValue;
}

    
