#include "JRIBootstrap.h"

#if defined WIN32 || defined Win32

#include <windows.h>
#include <winreg.h>

static const HKEY keyDB[2] = { HKEY_LOCAL_MACHINE, HKEY_CURRENT_USER };

JNIEXPORT jstring JNICALL Java_JRIBootstrap_getenv
(JNIEnv *env, jclass cl, jstring sVar) {
  char cVal[1024];
  int res;
  const char *cVar = (*env)->GetStringUTFChars(env, sVar, 0);
  if (!cVar) return 0;
  *cVal=0; cVal[1023]=0;
  res = GetEnvironmentVariable(cVar, cVal, 1023);
  (*env)->ReleaseStringUTFChars(env, sVar, cVar);
  return res?((*env)->NewStringUTF(env, cVal)):0;
}

JNIEXPORT void JNICALL Java_JRIBootstrap_setenv
(JNIEnv *env, jclass cl, jstring sVar, jstring sVal) {
  const char *cVar = sVar?(*env)->GetStringUTFChars(env, sVar, 0):0;
  const char *cVal = sVal?(*env)->GetStringUTFChars(env, sVal, 0):0;
  if (cVar) SetEnvironmentVariable(cVar, cVal?cVal:"");
  if (cVar) (*env)->ReleaseStringUTFChars(env, sVar, cVar);
  if (cVal) (*env)->ReleaseStringUTFChars(env, sVal, cVal);
  return;
}

JNIEXPORT jstring JNICALL Java_JRIBootstrap_regvalue
(JNIEnv *env, jclass cl, jint iRoot, jstring sKey, jstring sVal) {
  const char *cKey = sKey?(*env)->GetStringUTFChars(env, sKey, 0):0;
  const char *cVal = sVal?(*env)->GetStringUTFChars(env, sVal, 0):0;
  jstring res = 0;
  if (cKey && cVal) {
    HKEY key;
    if (RegOpenKeyEx(keyDB[iRoot], cKey, 0, KEY_QUERY_VALUE, &key) == ERROR_SUCCESS) {
      char buf[1024];
      DWORD t, s = 1023;
      *buf=0; buf[1023]=0;
      if (RegQueryValueEx(key, cVal, 0, &t, buf, &s) == ERROR_SUCCESS) {
	res = (*env)->NewStringUTF(env, buf);
      }
      RegCloseKey(key);
    }
  }
  if (cVal) (*env)->ReleaseStringUTFChars(env, sVal, cVal);
  if (cKey) (*env)->ReleaseStringUTFChars(env, sKey, cKey);
  
  return res;
}

JNIEXPORT jobjectArray JNICALL Java_JRIBootstrap_regsubkeys
(JNIEnv *env, jclass cl, jint iRoot, jstring sKey) {
  const char *cKey = sKey?(*env)->GetStringUTFChars(env, sKey, 0):0;
  jobjectArray res = 0;
  if (cKey) {
    HKEY key;
    if (RegOpenKeyEx(keyDB[iRoot], cKey, 0, KEY_ENUMERATE_SUB_KEYS|KEY_QUERY_VALUE, &key) == ERROR_SUCCESS) {
      int n = 0, i = 0;
      char buf[256];
      jclass cStr;
      *buf=0;
      buf[255]=0;
      /* pass 1: count the entries */
      while (RegEnumKey(key, n, buf, 254) == ERROR_SUCCESS) n++;
      /* pass 2: get the values */
      cStr = (*env)->FindClass(env, "java/lang/String");
      res = (*env)->NewObjectArray(env, n, cStr, 0);
      (*env)->DeleteLocalRef(env, cStr);
      while (i<n && RegEnumKey(key, i, buf, 254) == ERROR_SUCCESS)
	(*env)->SetObjectArrayElement(env, res, i++,
				      (*env)->NewStringUTF(env, buf));
      RegCloseKey(key);
    }
    (*env)->ReleaseStringUTFChars(env, sKey, cKey);
  }
  return res;
}

JNIEXPORT jstring JNICALL Java_JRIBootstrap_expand
(JNIEnv *env, jclass cl, jstring sVal) {
  jstring res = sVal;
  const char *cVal = sVal?(*env)->GetStringUTFChars(env, sVal, 0):0;
  char buf[1024];
  *buf=0; buf[1023]=0;
  if (cVal) {
    if (ExpandEnvironmentStrings(cVal, buf, 1023))
      res = (*env)->NewStringUTF(env, buf);
  }
  if (cVal) (*env)->ReleaseStringUTFChars(env, sVal, cVal);
  return res;
}

JNIEXPORT jboolean JNICALL Java_JRIBootstrap_hasreg
(JNIEnv *env, jclass cl) {
  return JNI_TRUE;
}

#else

#include <stdlib.h>

JNIEXPORT jstring JNICALL Java_JRIBootstrap_getenv
(JNIEnv *env, jclass cl, jstring sVar) {
  char *cVal;
  const char *cVar = sVar?(*env)->GetStringUTFChars(env, sVar, 0):0;
  if (!cVar) return 0;
  cVal=getenv(cVar);
  (*env)->ReleaseStringUTFChars(env, sVar, cVar);
  return cVal?((*env)->NewStringUTF(env, cVal)):0;
}

JNIEXPORT void JNICALL Java_JRIBootstrap_setenv
(JNIEnv *env, jclass cl, jstring sVar, jstring sVal) {
  const char *cVar = sVar?(*env)->GetStringUTFChars(env, sVar, 0):0;
  const char *cVal = sVal?(*env)->GetStringUTFChars(env, sVal, 0):0;
  if (cVar) setenv(cVar, cVal?cVal:"", 1);
  if (cVar) (*env)->ReleaseStringUTFChars(env, sVar, cVar);
  if (cVal) (*env)->ReleaseStringUTFChars(env, sVal, cVal);
  return;
}

/* no registry on unix, so always return null */
JNIEXPORT jstring JNICALL Java_JRIBootstrap_regvalue
(JNIEnv *env, jclass cl, jint iRoot, jstring sKey, jstring sVal) {
  return 0;
}

JNIEXPORT jobjectArray JNICALL Java_JRIBootstrap_regsubkeys
(JNIEnv *env, jclass cl, jint iRoot, jstring sKey) {
  return 0;
}

JNIEXPORT jstring JNICALL Java_JRIBootstrap_expand
(JNIEnv *env, jclass cl, jstring sVal) {
  return sVal;
}

JNIEXPORT jboolean JNICALL Java_JRIBootstrap_hasreg
(JNIEnv *env, jclass cl) {
  return JNI_FALSE;
}

#endif

JNIEXPORT jstring JNICALL Java_JRIBootstrap_arch
(JNIEnv *env, jclass cl) {
  const char *ca = "unknown";
  /* this is mainly for Macs so we can determine the correct arch ... */
#ifdef __ppc__
  ca = "ppc";
#endif
#ifdef __i386__
  ca = "i386";
#endif
#ifdef __x86_64__
  ca = "x86_64";
#endif
#ifdef __ppc64__
  ca = "ppc64";
#endif
  return (*env)->NewStringUTF(env, ca);
}
