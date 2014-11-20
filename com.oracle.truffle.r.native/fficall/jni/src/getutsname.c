#include <string.h>
#include <sys/utsname.h>
#include <jni.h>

struct utsname name;

static jfieldID checkGetFieldID(JNIEnv *env, jclass klass, const char *name, const char *sig) {
	jfieldID fieldID = (*env)->GetFieldID(env, klass, name, sig);
	if (fieldID == NULL) {
		char buf[1024];
		strcpy(buf, "failed to find field ");
		strcat(buf, name);
		(*env)->FatalError(env, buf);
	}
	return fieldID;
}

JNIEXPORT void JNICALL
Java_com_oracle_truffle_r_runtime_ffi_jnr_JNIUtsName_getutsname(JNIEnv *env, jobject obj) {
	uname(&name);
	jstring sysname = (*env)->NewStringUTF(env, name.sysname);
	jstring release = (*env)->NewStringUTF(env, name.release);
	jstring version = (*env)->NewStringUTF(env, name.version);
	jstring machine = (*env)->NewStringUTF(env, name.machine);

	jclass klass = (*env)->GetObjectClass(env, obj);

	jfieldID sysnameId = checkGetFieldID(env, klass, "sysname", "Ljava/lang/String;");
	jfieldID releaseId = checkGetFieldID(env, klass, "release", "Ljava/lang/String;");
	jfieldID versionId = checkGetFieldID(env, klass, "version", "Ljava/lang/String;");
	jfieldID machineId = checkGetFieldID(env, klass, "machine", "Ljava/lang/String;");

	(*env)->SetObjectField(env, obj, sysnameId, sysname);
	(*env)->SetObjectField(env, obj, releaseId, release);
	(*env)->SetObjectField(env, obj, versionId, version);
	(*env)->SetObjectField(env, obj, machineId, machine);

}
