/* R-callable functions to convert arrays into R objects
 *
 * rJava R/Java interface  (C)Copyright 2003-2007 Simon Urbanek
 * (see rJava project root for licensing details)
 */

#include "rJava.h"

/** get contents of the object array in the form of list of ext. pointers */
REPC SEXP RgetObjectArrayCont(SEXP e) {
  SEXP ar;
  jarray o;
  int l,i;
  JNIEnv *env=getJNIEnv();

  profStart();
  if (e==R_NilValue) return R_NilValue;
  if (TYPEOF(e)==EXTPTRSXP) {
    jverify(e);
    o=(jobject)EXTPTR_PTR(e);
  } else
    error("invalid object parameter");
  _dbg(rjprintf("RgetObjectArrayCont: jarray %x\n",o));
  if (!o) return R_NilValue;
  l=(int)(*env)->GetArrayLength(env, o);
  _dbg(rjprintf(" convert object array of length %d\n",l));
  if (l<1) return R_NilValue;
  PROTECT(ar=allocVector(VECSXP,l));
  i=0;
  while (i<l) {
    jobject ae = (*env)->GetObjectArrayElement(env, o, i);
    _mp(MEM_PROF_OUT("  %08x LNEW object array element [%d]\n", (int) ae, i))
    SET_VECTOR_ELT(ar, i, j2SEXP(env, ae, 1));
    i++;
  }
  UNPROTECT(1);
  _prof(profReport("RgetObjectArrayCont[%d]:",o));
  return ar;
}

/** get contents of the object array in the form of STRSXP vector */
REPC SEXP RgetStringArrayCont(SEXP e) {
  SEXP ar = R_NilValue ;
  if (e==R_NilValue) return R_NilValue;
  if (TYPEOF(e)==EXTPTRSXP) {
    jverify(e);
    ar = getStringArrayCont( (jarray)EXTPTR_PTR(e) ) ;
  } else
    error("invalid object parameter");
  return ar;
}

/** get contents of the integer array object */
REPC SEXP RgetIntArrayCont(SEXP e) {
  SEXP ar;
  jarray o;
  int l;
  jint *ap;
  JNIEnv *env=getJNIEnv();

  profStart();
  if (e==R_NilValue) return e;
  if (TYPEOF(e)==EXTPTRSXP) {
    jverify(e);
    o=(jobject)EXTPTR_PTR(e);
  } else
    error("invalid object parameter");
  _dbg(rjprintf("RgetIntArrayCont: jarray %x\n",o));
  if (!o) return R_NilValue;
  l=(int)(*env)->GetArrayLength(env, o);
  _dbg(rjprintf(" convert int array of length %d\n",l));
  if (l<0) return R_NilValue;
  ap=(jint*)(*env)->GetIntArrayElements(env, o, 0);
  if (!ap)
    error("cannot obtain integer array contents");
  PROTECT(ar=allocVector(INTSXP,l));
  if (l>0) memcpy(INTEGER(ar),ap,sizeof(jint)*l);
  UNPROTECT(1);
  (*env)->ReleaseIntArrayElements(env, o, ap, 0);
  _prof(profReport("RgetIntArrayCont[%d]:",o));
  return ar;
}

/** get contents of the boolean array object */
REPC SEXP RgetBoolArrayCont(SEXP e) {
  SEXP ar;
  jarray o;
  int l;
  jboolean *ap;
  JNIEnv *env=getJNIEnv();

  profStart();
  if (e==R_NilValue) return e;
  if (TYPEOF(e)==EXTPTRSXP) {
    jverify(e);
    o=(jobject)EXTPTR_PTR(e);
  } else
    error("invalid object parameter");
  _dbg(rjprintf("RgetBoolArrayCont: jarray %x\n",o));
  if (!o) return R_NilValue;
  l=(int)(*env)->GetArrayLength(env, o);
  _dbg(rjprintf(" convert bool array of length %d\n",l));
  if (l<0) return R_NilValue;
  ap=(jboolean*)(*env)->GetBooleanArrayElements(env, o, 0);
  if (!ap)
    error("cannot obtain boolean array contents");
  PROTECT(ar=allocVector(LGLSXP,l));
  { /* jboolean = byte, logical = int, need to convert */
    int i = 0;
    while (i < l) {
      LOGICAL(ar)[i] = ap[i];
      i++;
    }
  }
  UNPROTECT(1);
  (*env)->ReleaseBooleanArrayElements(env, o, ap, 0);
  _prof(profReport("RgetBoolArrayCont[%d]:",o));
  return ar;
}

/** get contents of a character array object */
REPC SEXP RgetCharArrayCont(SEXP e) {
  SEXP ar;
  jarray o;
  int l;
  jchar *ap;
  JNIEnv *env=getJNIEnv();

  profStart();
  if (e==R_NilValue) return e;
  if (TYPEOF(e)==EXTPTRSXP) {
    jverify(e);
    o=(jobject)EXTPTR_PTR(e);
  } else
    error("invalid object parameter");
  _dbg(rjprintf("RgetCharArrayCont: jarray %x\n",o));
  if (!o) return R_NilValue;
  l=(int)(*env)->GetArrayLength(env, o);
  _dbg(rjprintf(" convert char array of length %d\n",l));
  if (l<0) return R_NilValue;
  ap = (*env)->GetCharArrayElements(env, o, 0);
  if (!ap)
    error("cannot obtain char array contents");
  PROTECT(ar=allocVector(INTSXP,l));
  { /* jchar = 16-bit, need to convert */
    int i = 0;
    while (i < l) {
      INTEGER(ar)[i] = ap[i];
      i++;
    }
  }
  UNPROTECT(1);
  (*env)->ReleaseCharArrayElements(env, o, ap, 0);
  _prof(profReport("RgetCharArrayCont[%d]:",o));
  return ar;
}

/** get contents of a short array object */
REPC SEXP RgetShortArrayCont(SEXP e) {
  SEXP ar;
  jarray o;
  int l;
  jshort *ap;
  JNIEnv *env=getJNIEnv();

  profStart();
  if (e==R_NilValue) return e;
  if (TYPEOF(e)==EXTPTRSXP) {
    jverify(e);
    o=(jobject)EXTPTR_PTR(e);
  } else
    error("invalid object parameter");
  _dbg(rjprintf("RgetBoolArrayCont: jarray %x\n",o));
  if (!o) return R_NilValue;
  l=(int)(*env)->GetArrayLength(env, o);
  _dbg(rjprintf(" convert short array of length %d\n",l));
  if (l<0) return R_NilValue;
  ap = (*env)->GetShortArrayElements(env, o, 0);
  if (!ap)
    error("cannot obtain short array contents");
  PROTECT(ar=allocVector(INTSXP,l));
  { /* jshort = 16-bit, need to convert */
    int i = 0;
    while (i < l) {
      INTEGER(ar)[i] = ap[i];
      i++;
    }
  }
  UNPROTECT(1);
  (*env)->ReleaseShortArrayElements(env, o, ap, 0);
  _prof(profReport("RgetShortArrayCont[%d]:",o));
  return ar;
}

/** get contents of the byte array object */
REPC SEXP RgetByteArrayCont(SEXP e) {
  SEXP ar;
  jarray o;
  int l;
  jbyte *ap;
  JNIEnv *env=getJNIEnv();
  
  profStart();
  if (e==R_NilValue) return e;
  if (TYPEOF(e)==EXTPTRSXP) {
    jverify(e);
    o = (jobject)EXTPTR_PTR(e);
  } else
    error("invalid object parameter");
  _dbg(rjprintf("RgetByteArrayCont: jarray %x\n",o));
  if (!o) return R_NilValue;
  l=(int)(*env)->GetArrayLength(env, o);
  _dbg(rjprintf(" convert byte array of length %d\n",l));
  if (l<0) return R_NilValue;
  ap=(jbyte*)(*env)->GetByteArrayElements(env, o, 0);
  if (!ap)
    error("cannot obtain byte array contents");
  PROTECT(ar=allocVector(RAWSXP,l));
  if (l>0) memcpy(RAW(ar),ap,l);
  UNPROTECT(1);
  (*env)->ReleaseByteArrayElements(env, o, ap, 0);
  _prof(profReport("RgetByteArrayCont[%d]:",o));
  return ar;
}

/** get contents of the double array object  */
REPC SEXP RgetDoubleArrayCont(SEXP e) {
  SEXP ar;
  jarray o;
  int l;
  jdouble *ap;
  JNIEnv *env=getJNIEnv();

  profStart();
  if (e==R_NilValue) return e;
  if (TYPEOF(e)==EXTPTRSXP) {
    jverify(e);
    o = (jobject)EXTPTR_PTR(e);
  } else
    error("invalid object parameter");
  _dbg(rjprintf("RgetDoubleArrayCont: jarray %x\n",o));
  if (!o) return R_NilValue;
  l=(int)(*env)->GetArrayLength(env, o);
  _dbg(rjprintf(" convert double array of length %d\n",l));
  if (l<0) return R_NilValue;
  ap=(jdouble*)(*env)->GetDoubleArrayElements(env, o, 0);
  if (!ap)
    error("cannot obtain double array contents");
  PROTECT(ar=allocVector(REALSXP,l));
  if (l>0) memcpy(REAL(ar),ap,sizeof(jdouble)*l);
  UNPROTECT(1);
  (*env)->ReleaseDoubleArrayElements(env, o, ap, 0);
  _prof(profReport("RgetDoubleArrayCont[%d]:",o));
  return ar;
}

/** get contents of the float array object (double) */
REPC SEXP RgetFloatArrayCont(SEXP e) {
  SEXP ar;
  jarray o;
  int l;
  jfloat *ap;
  JNIEnv *env=getJNIEnv();

  profStart();
  if (e==R_NilValue) return e;
  if (TYPEOF(e)==EXTPTRSXP) {
    jverify(e);
    o = (jobject)EXTPTR_PTR(e);
  } else
    error("invalid object parameter");
  _dbg(rjprintf("RgetFloatArrayCont: jarray %d\n",o));
  if (!o) return R_NilValue;
  l=(int)(*env)->GetArrayLength(env, o);
  _dbg(rjprintf(" convert float array of length %d\n",l));
  if (l<0) return R_NilValue;
  ap=(jfloat*)(*env)->GetFloatArrayElements(env, o, 0);
  if (!ap)
    error("cannot obtain float array contents");
  PROTECT(ar=allocVector(REALSXP,l));
  { /* jfloat must be coerced into double .. we use just a cast for each element */
    int i=0;
    while (i<l) { REAL(ar)[i]=(double)ap[i]; i++; }
  }
  UNPROTECT(1);
  (*env)->ReleaseFloatArrayElements(env, o, ap, 0);
  _prof(profReport("RgetFloatArrayCont[%d]:",o));
  return ar;
}

/** get contents of the long array object (int) */
REPC SEXP RgetLongArrayCont(SEXP e) {
  SEXP ar;
  jarray o;
  int l;
  jlong *ap;
  JNIEnv *env=getJNIEnv();
	
  profStart();
  if (e==R_NilValue) return e;
  if (TYPEOF(e)==EXTPTRSXP) {
    jverify(e);
    o=(jobject)EXTPTR_PTR(e);
  } else
    error("invalid object parameter");
  _dbg(rjprintf("RgetLongArrayCont: jarray %d\n",o));
  if (!o) return R_NilValue;
  l=(int)(*env)->GetArrayLength(env, o);
  _dbg(rjprintf(" convert long array of length %d\n",l));
  if (l<0) return R_NilValue;
  ap=(jlong*)(*env)->GetLongArrayElements(env, o, 0);
  if (!ap)
    error("cannot obtain long contents");
  PROTECT(ar=allocVector(REALSXP,l));
  { /* long must be coerced into double .. we use just a cast for each element, bad idea? */
    int i=0;
    while (i<l) { REAL(ar)[i]=(double)ap[i]; i++; }
  }
  UNPROTECT(1);
  (*env)->ReleaseLongArrayElements(env, o, ap, 0);
  _prof(profReport("RgetLongArrayCont[%d]:",o));
  return ar;
}


/* these below have been factored out of the ones above so that they 
   can also be used internally in jni code */
   
/** 
 * get contents of the String array in the form of STRSXP vector
 * 
 * @param e a pointer to a String[] object
 * 
 * @return a STRSXP vector mirroring the java array
 */
HIDE SEXP getStringArrayCont(jarray o) {
	SEXP ar;
  int l,i;
  const char *c;
  JNIEnv *env=getJNIEnv();

  profStart();
  
  _dbg(rjprintf("RgetStringArrayCont: jarray %x\n",o));
  if (!o) return R_NilValue;
  l=(int)(*env)->GetArrayLength(env, o);
  _dbg(rjprintf(" convert string array of length %d\n",l));
  if (l<0) return R_NilValue;
  PROTECT(ar=allocVector(STRSXP,l));
  i=0;
  while (i<l) {
    jobject sobj=(*env)->GetObjectArrayElement(env, o, i);
    _mp(MEM_PROF_OUT("  %08x LNEW object array element [%d]\n", (int) sobj, i))
    c=0;
    if (sobj) {
      /* we could (should?) check the type here ...
      if (!(*env)->IsInstanceOf(env, sobj, javaStringClass)) {
	printf(" not a String\n");
      } else
      */
      c=(*env)->GetStringUTFChars(env, sobj, 0);
    }
    if (!c)
      SET_STRING_ELT(ar, i, R_NaString);
    else {
      SET_STRING_ELT(ar, i, mkCharUTF8(c));
      (*env)->ReleaseStringUTFChars(env, sobj, c);
    }
    if (sobj) releaseObject(env, sobj);
    i++;
  }
  UNPROTECT(1);
  _prof(profReport("RgetStringArrayCont[%d]:",o))
  return ar;
}


/**
 *  Get the list of class names of a java object
 * This is a jni wrapper around the RJavaTools.getSimpleClassNames method
 */
HIDE jarray getSimpleClassNames( jobject o, jboolean addConditionClasses ){

	JNIEnv *env=getJNIEnv();
	jarray a ;
	
	profStart();
	a = (jarray) (*env)->CallStaticObjectMethod(env, 
		rj_RJavaTools_Class, mid_rj_getSimpleClassNames, 
		o, addConditionClasses ) ;
    _prof(profReport("getSimpleClassNames[%d]:",o)) ;
	return a ;
}

/** 
 * Get the list of class names of a java object, and
 * structure it as a STRSXP vector
 */ 
HIDE SEXP getSimpleClassNames_asSEXP( jobject o, jboolean addConditionClasses ){
	if( !o ){
		SEXP res = PROTECT( allocVector( STRSXP, 4) ) ;
		SET_STRING_ELT( res, 0, mkChar( "Exception" ) ) ; 
		SET_STRING_ELT( res, 1, mkChar( "Throwable" ) ) ; 
		SET_STRING_ELT( res, 2, mkChar( "error" ) ) ; 
		SET_STRING_ELT( res, 3, mkChar( "condition" ) ) ; 
		UNPROTECT(1);
		return res ;
	} else{
		return getStringArrayCont( getSimpleClassNames( o, addConditionClasses ) );
	}
}

/**
 * Returns the STRSXP vector of simple class names of the object o
 */
REPC SEXP RgetSimpleClassNames( SEXP e, SEXP addConditionClasses ){
	
	SEXP ar = R_NilValue ;
  	if (e==R_NilValue) return R_NilValue;
  	jobject jobj ;
  	if (TYPEOF(e)==EXTPTRSXP) {
  		jverify(e);
  		jobj = (jobject)EXTPTR_PTR(e) ;
  	} else {
  		error("invalid object parameter");
  	}
	
 	Rboolean add ;
	switch(TYPEOF(addConditionClasses)) {
		case LGLSXP:
		    add = LOGICAL(addConditionClasses)[0];
		    break;
		case INTSXP:
		    add = INTEGER(addConditionClasses)[0]; 
		    break;
		default:
		    add = asLogical(addConditionClasses);
	}
	
  ar = getSimpleClassNames_asSEXP( jobj , (jboolean)add ) ;

  return ar;
}

