/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

#include <assert.h>
#include <rffiutils.h>
#include <R_ext/Connections.h>


static jmethodID readConnMethodID;
static jmethodID writeConnMethodID;
static jmethodID getConnMethodID;
static jmethodID getConnClassMethodID;
static jmethodID getSummaryDescMethodID;
static jmethodID isSeekableMethodID;
static jmethodID getOpenModeMethodID;

static jbyteArray wrap(JNIEnv *thisenv, void* buf, size_t n) {
    jbyteArray barr = (*thisenv)->NewByteArray(thisenv, n);
    (*thisenv)->SetByteArrayRegion(thisenv, barr, 0, n, buf);
    return barr;
}

/*
 * Returns the file descriptor of the connection if possible.
 * Otherwise an error is issued.
 */
static int getFd(Rconnection con) {
	if(strstr(con->class, "file") != NULL ) {
		return *((int*)(con->private));
	}
	error(_("cannot get file descriptor for non-file connection"));
	return -1;
}

/*
 * Sets the file descriptor for the connection.
 */
static void setFd(Rconnection con, jint fd) {
	*((int*)(con->private)) = fd;
}


void init_connections(JNIEnv *env) {
	/* int readConn(int, byte[]) */
	readConnMethodID = checkGetMethodID(env, UpCallsRFFIClass, "R_ReadConnection", "(I[B)I", 0);

	/* int writeConn(int, byte[]) */
	writeConnMethodID = checkGetMethodID(env, UpCallsRFFIClass, "R_WriteConnection", "(I[B)I", 0);

	/* RConnection getConnection(int) */
	getConnMethodID = checkGetMethodID(env, UpCallsRFFIClass, "R_GetConnection", "(I)Ljava/lang/Object;", 0);


	/* String getConnectionClassString(BaseRConnection) */
	getConnClassMethodID = checkGetMethodID(env, UpCallsRFFIClass, "getConnectionClassString", "(Ljava/lang/Object;)Ljava/lang/String;", 0);

	/* String getSummaryDescription(BaseRConnection) */
	getSummaryDescMethodID = checkGetMethodID(env, UpCallsRFFIClass, "getSummaryDescription", "(Ljava/lang/Object;)Ljava/lang/String;", 0);

	/* boolean isSeekable(BaseRConnection) */
	isSeekableMethodID = checkGetMethodID(env, UpCallsRFFIClass, "isSeekable", "(Ljava/lang/Object;)Z", 0);

	/* String getOpenModeString(BaseRConnection) */
	getOpenModeMethodID = checkGetMethodID(env, UpCallsRFFIClass, "getOpenModeString", "(Ljava/lang/Object;)Ljava/lang/String;", 0);
}

static char *connStringToChars(JNIEnv *env, jstring string) {
    jsize len = (*env)->GetStringUTFLength(env, string);
    const char *stringChars = (*env)->GetStringUTFChars(env, string, NULL);
    char *copyChars = malloc((len + 1)*sizeof(char));
    memcpy(copyChars, stringChars, len*sizeof(char));
    copyChars[len] = 0;
	(*env)->ReleaseStringUTFChars(env, string, stringChars);
	(*env)->DeleteLocalRef(env, string);
    return copyChars;
}

/* ------------------- null connection functions --------------------- */

static Rboolean NORET null_open(Rconnection con)
{
    error(_("%s not enabled for this connection"), "open");
}

static void null_close(Rconnection con)
{
    con->isopen = FALSE;
}

static void null_destroy(Rconnection con)
{
    if(con->private) free(con->private);
}

static int NORET null_vfprintf(Rconnection con, const char *format, va_list ap)
{
    error(_("%s not enabled for this connection"), "printing");
}

static int NORET null_fgetc(Rconnection con)
{
    error(_("%s not enabled for this connection"), "'getc'");
}

static double NORET null_seek(Rconnection con, double where, int origin, int rw)
{
    error(_("%s not enabled for this connection"), "'seek'");
}

static void NORET null_truncate(Rconnection con)
{
    error(_("%s not enabled for this connection"), "truncation");
}

static int null_fflush(Rconnection con)
{
}

static size_t NORET null_read(void *ptr, size_t size, size_t nitems,
			Rconnection con)
{
    error(_("%s not enabled for this connection"), "'read'");
}

static size_t NORET null_write(const void *ptr, size_t size, size_t nitems,
			 Rconnection con)
{
    error(_("%s not enabled for this connection"), "'write'");
}

static void init_con(Rconnection new, char *description, int enc,
	      const char * const mode)
{
    new->description = description;
    new->enc = enc;
    strncpy(new->mode, mode, 4); new->mode[4] = '\0';
    new->isopen = new->incomplete = new->blocking = new->isGzcon = FALSE;
    new->canread = new->canwrite = TRUE; /* in principle */
    new->canseek = FALSE;
    new->text = TRUE;
    new->open = &null_open;
    new->close = &null_close;
    new->destroy = &null_destroy;
    new->vfprintf = &null_vfprintf;
    new->fgetc = new->fgetc_internal = &null_fgetc;
    new->seek = &null_seek;
    new->truncate = &null_truncate;
    new->fflush = &null_fflush;
    new->read = &null_read;
    new->write = &null_write;
    new->nPushBack = 0;
    new->save = new->save2 = -1000;
    new->private = NULL;
    new->inconv = new->outconv = NULL;
    new->UTF8out = FALSE;
    new->id = 0;
    new->ex_ptr = NULL;
    new->status = NA_INTEGER;
}

SEXP R_new_custom_connection(const char *description, const char *mode, const char *class_name, Rconnection *ptr) {
	return unimplemented("R_new_custom_connection");
}

size_t R_ReadConnection(Rconnection con, void *buf, size_t n) {
    JNIEnv *thisenv = getEnv();
    jbyteArray barr = (*thisenv)->NewByteArray(thisenv, n);

    jint result =  (*thisenv)->CallIntMethod(thisenv, UpCallsRFFIObject, readConnMethodID, getFd(con), barr);
    size_t readBytes = result >= 0 ? (size_t) result : 0;
    assert(result <= (ssize_t) n);
    if(result > 0) {
    	(*thisenv)->GetByteArrayRegion(thisenv, barr, 0, result, buf);
    }
    return readBytes;
}

size_t R_WriteConnection(Rconnection con, void *buf, size_t n) {
    JNIEnv *thisenv = getEnv();
    jbyteArray barr = wrap(thisenv, buf, n);

    jint result =  (*thisenv)->CallIntMethod(thisenv, UpCallsRFFIObject, writeConnMethodID, getFd(con), barr);
    return result >= 0 ? (size_t) result : 0;
}

Rconnection R_GetConnection(SEXP sConn) {
	if (!inherits(sConn, "connection")) {
		error(_("invalid connection"));
	}

	int fd = asInteger(sConn);

	JNIEnv *thisenv = getEnv();
	jobject jRconn = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, getConnMethodID, fd);

	// query getConnectionClassString
	jstring jConnClass = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, getConnClassMethodID, jRconn);
	const char *sConnClass;
	if (jConnClass != 0) {
		sConnClass = connStringToChars(thisenv, jConnClass);
	}

	// query getSummaryDescription
	jstring jSummaryDesc = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, getSummaryDescMethodID, jRconn);
	char *sSummaryDesc;
	if (jSummaryDesc != 0) {
		sSummaryDesc = connStringToChars(thisenv, jSummaryDesc);
	}

	// query isSeekable()
	jboolean seekable = (*thisenv)->CallBooleanMethod(thisenv, UpCallsRFFIObject, isSeekableMethodID, jRconn);

	// query getOpenMode
	jstring jOpenMode = (*thisenv)->CallObjectMethod(thisenv, UpCallsRFFIObject, getOpenModeMethodID, jRconn);
	char *sOpenMode;
	if (jOpenMode != 0) {
		sOpenMode = connStringToChars(thisenv, jOpenMode);
	}

	Rconnection new = (Rconnection) malloc(sizeof(struct Rconn));
	if (!new) {
		error(_("allocation of file connection failed"));
	}

	init_con(new, sSummaryDesc, 0, sOpenMode);
	free(sOpenMode);
	new->class = sConnClass;
	new->canseek = seekable;

	/* The private field is forbidden to be used by any user. So we can use it to store the file descriptor. However, we should also use Rfileconn in future. */
	new->private = (void *) malloc(sizeof(int));
	if (!new->private) {
		free(new);
		error(_("allocation of file connection failed"));
		/* for Solaris 12.5 */new = NULL;
	}
	setFd(new, fd);

// TODO implement up-call functions and set them
//    new->open = &file_open;
//    new->close = &file_close;
//    new->vfprintf = &file_vfprintf;
//    new->fgetc_internal = &file_fgetc_internal;
//    new->fgetc = &dummy_fgetc;
//    new->seek = &file_seek;
//    new->truncate = &file_truncate;
//    new->fflush = &file_fflush;
//    new->read = &file_read;
//    new->write = &file_write;

	return new;
}

