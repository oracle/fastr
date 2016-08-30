#include "gramRd_fastr.h"
#include <jni.h>

extern JNIEnv *getEnv();

static jmethodID getcMethodID = NULL;

static void findGetCMethod(JNIEnv *env) {
    jclass klass = (*env)->FindClass(env, "com/oracle/truffle/r/runtime/conn/RConnection");
    getcMethodID = (*env)->GetMethodID(env, klass, "getc", "()I");
}

int callGetCMethod(void *conn) {
    JNIEnv *env = getEnv();
	if (getcMethodID == NULL) {
		findGetCMethod(env);
	}
    int c = (*env)->CallIntMethod(env, conn, getcMethodID, conn);
    return c;
}
