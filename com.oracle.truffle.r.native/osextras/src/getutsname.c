/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
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
Java_com_oracle_truffle_r_runtime_ffi_jnr_JNI_1UtsName_getutsname(JNIEnv *env, jobject obj) {
	uname(&name);
	jstring sysname = (*env)->NewStringUTF(env, name.sysname);
	jstring release = (*env)->NewStringUTF(env, name.release);
	jstring version = (*env)->NewStringUTF(env, name.version);
	jstring machine = (*env)->NewStringUTF(env, name.machine);
	jstring nodename = (*env)->NewStringUTF(env, name.nodename);

	jclass klass = (*env)->GetObjectClass(env, obj);

	jfieldID sysnameId = checkGetFieldID(env, klass, "sysname", "Ljava/lang/String;");
	jfieldID releaseId = checkGetFieldID(env, klass, "release", "Ljava/lang/String;");
	jfieldID versionId = checkGetFieldID(env, klass, "version", "Ljava/lang/String;");
	jfieldID machineId = checkGetFieldID(env, klass, "machine", "Ljava/lang/String;");
	jfieldID nodenameId = checkGetFieldID(env, klass, "nodename", "Ljava/lang/String;");

	(*env)->SetObjectField(env, obj, sysnameId, sysname);
	(*env)->SetObjectField(env, obj, releaseId, release);
	(*env)->SetObjectField(env, obj, versionId, version);
	(*env)->SetObjectField(env, obj, machineId, machine);
	(*env)->SetObjectField(env, obj, nodenameId, nodename);

}
