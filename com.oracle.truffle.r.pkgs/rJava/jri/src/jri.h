#ifndef __JRI_H__
#define __JRI_H__

#include <jni.h>
#include <R.h>
#include <Rinternals.h>
#include <Rdefines.h>
#include <Rversion.h>

/* the viewpoint is from R, i.e. "get" means "Java->R" whereas "put" means "R->Java" */

#define JRI_VERSION 0x0505 /* JRI v0.5-5 */
#define JRI_API     0x010a /* API-version 1.10 */

#ifdef __cplusplus
extern "C" {
#endif
    
  /* jlong can always hold a pointer 
     to avoid warnings we go ptr->ulong->jlong */
#define SEXP2L(s) ((jlong)(s))
#ifdef WIN64
#define L2SEXP(s) ((SEXP)((jlong)(s)))
#else
#define L2SEXP(s) ((SEXP)((jlong)((unsigned long)(s))))
#endif

jstring jri_callToString(JNIEnv *env, jobject o);

SEXP jri_getDoubleArray(JNIEnv *env, jarray o);
SEXP jri_getIntArray(JNIEnv *env, jarray o);
SEXP jri_getByteArray(JNIEnv *env, jarray o);
SEXP jri_getBoolArrayI(JNIEnv *env, jarray o);
SEXP jri_getBoolArray(JNIEnv *env, jarray o);
SEXP jri_getObjectArray(JNIEnv *env, jarray o);
SEXP jri_getString(JNIEnv *env, jstring s);
SEXP jri_getStringArray(JNIEnv *env, jarray o);
SEXP jri_getSEXPLArray(JNIEnv *env, jarray o);

SEXP jri_installString(JNIEnv *env, jstring s); /* as Rf_install, just for Java strings */

jarray  jri_putDoubleArray(JNIEnv *env, SEXP e);
jarray  jri_putIntArray(JNIEnv *env, SEXP e);
jarray  jri_putBoolArrayI(JNIEnv *env, SEXP e);
jarray  jri_putByteArray(JNIEnv *env, SEXP e);
jstring jri_putString(JNIEnv *env, SEXP e, int ix); /* ix=index, 0=1st */
jarray  jri_putStringArray(JNIEnv *env, SEXP e);
jarray jri_putSEXPLArray(JNIEnv *env, SEXP e); /* SEXPs are strored as "long"s */

jstring jri_putSymbolName(JNIEnv *env, SEXP e);

void jri_checkExceptions(JNIEnv *env, int describe);

void jri_error(char *fmt, ...);

/* define mkCharUTF8 in a compatible fashion */
#if R_VERSION < R_Version(2,7,0)
#define mkCharUTF8(X) mkChar(X)
#define CHAR_UTF8(X) CHAR(X)
#else
#define mkCharUTF8(X) mkCharCE(X, CE_UTF8)
#define CHAR_UTF8(X) jri_char_utf8(X)
const char *jri_char_utf8(SEXP);
#endif

#ifdef __cplusplus
}
#endif

#endif

/*
   API version changes:
 -----------------------
   1.3 (initial public API version)
 [ 1.4 never publicly released - added put/getenv but was abandoned ]
   1.5 JRI 0.3-0
       + rniGetTAG
       + rniInherits
       + rniGetSymbolName
       + rniInstallSymbol
       + rniJavaToXref, rniXrefToJava
   1.6 JRI 0.3-2
       + rniPutBoolArray, rniPutBoolArrayI, rniGetBoolArrayI
   1.7 JRI 0.3-7
       + rniCons(+2 args)
   1.8 JRI 0.4-0
       + rniPrint
   1.9 JRI 0.4-3
       + rniPreserve, rniRelease
       + rniParentEnv, rniFindVar, rniListEnv
       + rniSpecialObject(0-7)
       + rniPrintValue
    1.10 JRI 0.5-1
       * rniAssign returns jboolean instead of void
*/
