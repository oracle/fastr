#include <stdarg.h>
#include <string.h>
#include "rJava.h"
#include <R_ext/Print.h>
#include <R_ext/Error.h>

#ifdef MEMPROF
FILE *memprof_f = 0;
#endif

/* local to JRI */
static void releaseLocal(JNIEnv *env, jobject o);

REPC SEXP RJavaCheckExceptions(SEXP silent) {
    int result = 0;
    JNIEnv *env = getJNIEnv();
    if (env)
	result = checkExceptionsX(env, asInteger(silent));
    return ScalarInteger(result);
}

HIDE void* errJNI(const char *err, ...) {
	char msg[512];
	va_list ap;
#ifndef RJ_DEBUG
	/* non-debug version goes straight to ckx - it should never return */
	ckx(NULL);
#endif
	va_start(ap, err);
	msg[511]=0;
	vsnprintf(msg, 511, err, ap);
#ifdef RJ_DEBUG
	Rf_warning(msg);
#else
	Rf_error(msg);
	/* this never returns and is just a fallback in case ckx doesn't return */
#endif
	va_end(ap);
	checkExceptionsX(getJNIEnv(), 0);
	return 0;
}

HIDE jclass findClass(JNIEnv *env, const char *cName) {
  if (clClassLoader) {
    char cn[128], *c=cn;
    jobject cns;
    jclass cl;

    strcpy(cn, cName);
    while (*c) { if (*c=='/') *c='.'; c++; };
    cns = newString(env, cn);
    if (!cns) error("unable to create Java string from '%s'", cn);
#ifdef DEBUG_CL
    printf("findClass(\"%s\") [with rJava loader]\n", cn);
#endif
    cl = (jclass) (*env)->CallStaticObjectMethod(env, javaClassClass, mid_forName, cns, (jboolean) 1, oClassLoader);
#if RJAVA_LEGACY
    clx(env);
#endif
    _mp(MEM_PROF_OUT("  %08x LNEW class\n", (int) cl))
    releaseObject(env, cns);
#ifdef DEBUG_CL
    printf(" - got %x\n", (unsigned int) cl);
#endif
#if RJAVA_LEGACY
    if (cl)
#endif
	return cl;
  }
#ifdef DEBUG_CL
  printf("findClass(\"%s\") (no loader)\n", cName);
#endif
  { 
    jclass cl = (*env)->FindClass(env, cName);
    _mp(MEM_PROF_OUT("  %08x LNEW class\n", (int) cl))
#ifdef DEBUG_CL
    printf(" - got %x\n", (unsigned int) cl); 
#endif
    return cl;
  }
}

HIDE jobject createObject(JNIEnv *env, const char *class, const char *sig, jvalue *par, int silent) {
  /* va_list ap; */
  jmethodID mid;
  jclass cls;
  jobject o;

  cls=findClass(env, class);
  if (!cls) return silent?0:errJNI("createObject.FindClass %s failed",class);
  mid=(*env)->GetMethodID(env, cls, "<init>", sig);
  if (!mid) {
    releaseLocal(env, cls);  
    return silent?0:errJNI("createObject.GetMethodID(\"%s\",\"%s\") failed",class,sig);
  }
  
  /*  va_start(ap, sig); */
  o=(*env)->NewObjectA(env, cls, mid, par);
  _mp(MEM_PROF_OUT("  %08x LNEW object\n", (int) o))
  /* va_end(ap); */
  releaseLocal(env, cls);  
  
  return (o||silent)?o:errJNI("NewObject(\"%s\",\"%s\",...) failed",class,sig);
}

HIDE void printObject(JNIEnv *env, jobject o) {
  jmethodID mid;
  jclass cls;
  jobject s;
  const char *c;

  cls=(*env)->GetObjectClass(env,o);
  _mp(MEM_PROF_OUT("  %08x LNEW class from object %08x (JRI-local)\n", (int)cls, (int)o))
  if (!cls) { releaseLocal(env, cls); errJNI("printObject.GetObjectClass failed"); return ; }
  mid=(*env)->GetMethodID(env, cls, "toString", "()Ljava/lang/String;");
  if (!mid) { releaseLocal(env, cls); errJNI("printObject.GetMethodID for toString() failed"); return; }
  s=(*env)->CallObjectMethod(env, o, mid);
  _mp(MEM_PROF_OUT("  %08x LNEW object method toString result (JRI-local)\n", (int)s))
  if (!s) { releaseLocal(env, cls); errJNI("printObject o.toString() call failed"); return; }
  c=(*env)->GetStringUTFChars(env, (jstring)s, 0);
  (*env)->ReleaseStringUTFChars(env, (jstring)s, c);
  releaseLocal(env, cls);  
  releaseLocal(env, s);
}

HIDE jdoubleArray newDoubleArray(JNIEnv *env, double *cont, int len) {
  jdoubleArray da=(*env)->NewDoubleArray(env,len);
  jdouble *dae;

  _mp(MEM_PROF_OUT("  %08x LNEW double[%d]\n", (int) da, len))
  if (!da) return errJNI("newDoubleArray.new(%d) failed",len);
  dae=(*env)->GetDoubleArrayElements(env, da, 0);
  if (!dae) {
    releaseLocal(env, da);
    return errJNI("newDoubleArray.GetDoubleArrayElements failed");
  }
  memcpy(dae,cont,sizeof(jdouble)*len);
  (*env)->ReleaseDoubleArrayElements(env, da, dae, 0);
  return da;
}

HIDE jintArray newIntArray(JNIEnv *env, int *cont, int len) {
  jintArray da=(*env)->NewIntArray(env,len);
  jint *dae;

  _mp(MEM_PROF_OUT("  %08x LNEW int[%d]\n", (int) da, len))
  if (!da) return errJNI("newIntArray.new(%d) failed",len);
  dae=(*env)->GetIntArrayElements(env, da, 0);
  if (!dae) {
    releaseLocal(env,da);
    return errJNI("newIntArray.GetIntArrayElements failed");
  }
  memcpy(dae,cont,sizeof(jint)*len);
  (*env)->ReleaseIntArrayElements(env, da, dae, 0);
  return da;
}

HIDE jbyteArray newByteArray(JNIEnv *env, void *cont, int len) {
  jbyteArray da=(*env)->NewByteArray(env,len);
  jbyte *dae;

  _mp(MEM_PROF_OUT("  %08x LNEW byte[%d]\n", (int) da, len))
  if (!da) return errJNI("newByteArray.new(%d) failed",len);
  dae=(*env)->GetByteArrayElements(env, da, 0);
  if (!dae) {
    releaseLocal(env,da);
    return errJNI("newByteArray.GetByteArrayElements failed");
  }
  memcpy(dae,cont,len);
  (*env)->ReleaseByteArrayElements(env, da, dae, 0);
  return da;
}

HIDE jbyteArray newByteArrayI(JNIEnv *env, int *cont, int len) {
  jbyteArray da=(*env)->NewByteArray(env,len);
  jbyte* dae;
  int i=0;

  _mp(MEM_PROF_OUT("  %08x LNEW byte[%d]\n", (int) da, len))
  if (!da) return errJNI("newByteArray.new(%d) failed",len);
  dae=(*env)->GetByteArrayElements(env, da, 0);
  if (!dae) {
    releaseLocal(env,da);
    return errJNI("newByteArray.GetByteArrayElements failed");
  }
  while (i<len) {
    dae[i]=(jbyte)cont[i];
    i++;
  }
  (*env)->ReleaseByteArrayElements(env, da, dae, 0);
  return da;
}

HIDE jbooleanArray newBooleanArrayI(JNIEnv *env, int *cont, int len) {
  jbooleanArray da=(*env)->NewBooleanArray(env,len);
  jboolean *dae;
  int i=0;

  _mp(MEM_PROF_OUT("  %08x LNEW bool[%d]\n", (int) da, len))
  if (!da) return errJNI("newBooleanArrayI.new(%d) failed",len);
  dae=(*env)->GetBooleanArrayElements(env, da, 0);
  if (!dae) {
    releaseLocal(env,da);
    return errJNI("newBooleanArrayI.GetBooleanArrayElements failed");
  }
  /* we cannot just memcpy since JNI uses unsigned char and R uses int */
  while (i<len) {
    dae[i]=(jboolean)cont[i];
    i++;
  }
  (*env)->ReleaseBooleanArrayElements(env, da, dae, 0);
  return da;
}

HIDE jcharArray newCharArrayI(JNIEnv *env, int *cont, int len) {
  jcharArray da=(*env)->NewCharArray(env,len);
  jchar *dae;
  int i=0;

  _mp(MEM_PROF_OUT("  %08x LNEW char[%d]\n", (int) da, len))
  if (!da) return errJNI("newCharArrayI.new(%d) failed",len);
  dae=(*env)->GetCharArrayElements(env, da, 0);
  if (!dae) {
    releaseLocal(env,da);
    return errJNI("newCharArrayI.GetCharArrayElements failed");
  }
  while (i<len) {
    dae[i]=(jchar)cont[i];
    i++;
  }
  (*env)->ReleaseCharArrayElements(env, da, dae, 0);
  return da;
}

HIDE jshortArray newShortArrayI(JNIEnv *env, int *cont, int len) {
  jshortArray da=(*env)->NewShortArray(env,len);
  jshort *dae;
  int i=0;

  _mp(MEM_PROF_OUT("  %08x LNEW short[%d]\n", (int) da, len))
  if (!da) return errJNI("newShortArrayI.new(%d) failed",len);
  dae=(*env)->GetShortArrayElements(env, da, 0);
  if (!dae) {
    releaseLocal(env,da);
    return errJNI("newShortArrayI.GetShortArrayElements failed");
  }
  while (i<len) {
    dae[i]=(jshort)cont[i];
    i++;
  }
  (*env)->ReleaseShortArrayElements(env, da, dae, 0);
  return da;
}

HIDE jfloatArray newFloatArrayD(JNIEnv *env, double *cont, int len) {
  jfloatArray da=(*env)->NewFloatArray(env,len);
  jfloat *dae;
  int i=0;

  _mp(MEM_PROF_OUT("  %08x LNEW float[%d]\n", (int) da, len))
  if (!da) return errJNI("newFloatArrayD.new(%d) failed",len);
  dae=(*env)->GetFloatArrayElements(env, da, 0);
  if (!dae) {
    releaseLocal(env,da);
    return errJNI("newFloatArrayD.GetFloatArrayElements failed");
  }
  /* we cannot just memcpy since JNI uses float and R uses double */
  while (i<len) {
    dae[i]=(jfloat)cont[i];
    i++;
  }
  (*env)->ReleaseFloatArrayElements(env, da, dae, 0);
  return da;
}

HIDE jlongArray newLongArrayD(JNIEnv *env, double *cont, int len) {
	jlongArray da=(*env)->NewLongArray(env,len);
	jlong *dae;
	int i=0;
	
	_mp(MEM_PROF_OUT("  %08x LNEW long[%d]\n", (int) da, len))
	if (!da) return errJNI("newLongArrayD.new(%d) failed",len);
	dae=(*env)->GetLongArrayElements(env, da, 0);
	if (!dae) {
	  releaseLocal(env, da);
	  return errJNI("newLongArrayD.GetFloatArrayElements failed");
	}
	/* we cannot just memcpy since JNI uses long and R uses double */
	while (i<len) {
		/* we're effectively rounding to prevent representation issues
		   however, we still may introduce some errors this way */
		dae[i]=(jlong)(cont[i]+0.5);
		i++;
	}
	(*env)->ReleaseLongArrayElements(env, da, dae, 0);
	return da;
}

HIDE jstring newString(JNIEnv *env, const char *cont) {
  jstring s=(*env)->NewStringUTF(env, cont);
  _mp(MEM_PROF_OUT("  %08x LNEW string \"%s\"\n", (int) s, cont))
  return s?s:errJNI("newString(\"%s\") failed",cont);
}

HIDE void releaseObject(JNIEnv *env, jobject o) {
  /* Rprintf("releaseObject: %lx\n", (long)o);
     printObject(env, o); */
  _mp(MEM_PROF_OUT("  %08x LREL\n", (int)o))
  (*env)->DeleteLocalRef(env, o);
}

HIDE jclass objectClass(JNIEnv *env, jobject o) {
  jclass cls=(*env)->GetObjectClass(env,o);
  _mp(MEM_PROF_OUT("  %08x LNEW class from object %08x\n", (int) cls, (int) o))
    return cls;
}  

static void releaseLocal(JNIEnv *env, jobject o) {
  _mp(MEM_PROF_OUT("  %08x LREL (JRI-local)\n", (int)o))
  (*env)->DeleteLocalRef(env, o);
}

HIDE jobject makeGlobal(JNIEnv *env, jobject o) {
  jobject g=(*env)->NewGlobalRef(env,o);
  _mp(MEM_PROF_OUT("G %08x GNEW %08x\n", (int) g, (int) o))
  return g?g:errJNI("makeGlobal: failed to make global reference");
}

HIDE void releaseGlobal(JNIEnv *env, jobject o) {
  /* Rprintf("releaseGlobal: %lx\n", (long)o);
     printObject(env, o); */
  _mp(MEM_PROF_OUT("G %08x GREL\n", (int) o))
  (*env)->DeleteGlobalRef(env,o);
}

static jobject nullEx = 0;

HIDE int checkExceptionsX(JNIEnv *env, int silent) {
  jthrowable t=(*env)->ExceptionOccurred(env);
  
  if (t == nullEx) t = 0; else {
    if ((*env)->IsSameObject(env, t, 0)) {
      nullEx = t; t = 0;
    } else {
      _mp(MEM_PROF_OUT("  %08x LNEW exception\n", (int) t))
    }
  }

  if (t) {
	  if (!silent) 
		  ckx(env);
    (*env)->ExceptionClear(env);
    releaseLocal(env, t);
    return 1;
  }
  return 0;
}
