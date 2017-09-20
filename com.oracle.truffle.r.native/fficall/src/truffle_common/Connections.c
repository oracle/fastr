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

#include <Defn.h>
#include <R_ext/Connections.h>
// Note: R_ext/Connections.h depends on Defn.h,
// but does not include it -> the order is important

#include "rffi_upcalls.h"
#include "../truffle_nfi/rffiutils.h"

/*
 * Returns the file descriptor of the connection if possible.
 * Otherwise an error is issued.
 */
static int getFd(Rconnection con) {
    return (int) con->id;
}

/*
 * Sets the file descriptor for the connection.
 */
static void setFd(Rconnection con, int fd) {
    con->id = (void *) (long) fd;
}

/* ------------------- null connection functions --------------------- */

static Rboolean NORET null_open(Rconnection con) {
    error(_("%s not enabled for this connection"), "open");
}

static void null_close(Rconnection con) {
    con->isopen = FALSE;
}

static void null_destroy(Rconnection con) {
    if (con->private) free(con->private);
}

static int NORET null_vfprintf(Rconnection con, const char *format, va_list ap) {
    error(_("%s not enabled for this connection"), "printing");
}

static int NORET null_fgetc(Rconnection con) {
    error(_("%s not enabled for this connection"), "'getc'");
}

static double NORET null_seek(Rconnection con, double where, int origin, int rw) {
    error(_("%s not enabled for this connection"), "'seek'");
}

static void NORET null_truncate(Rconnection con) {
    error(_("%s not enabled for this connection"), "truncation");
}

static int null_fflush(Rconnection con) {
    return 0;
}

static size_t NORET null_read(void *ptr, size_t size, size_t nitems,
                              Rconnection con) {
    error(_("%s not enabled for this connection"), "'read'");
}

static size_t NORET null_write(const void *ptr, size_t size, size_t nitems,
                               Rconnection con) {
    error(_("%s not enabled for this connection"), "'write'");
}

/* ------------------- connection structure functions --------------------- */

static void init_con(Rconnection new, char *description, int enc,
                     const char *const mode) {
    new->description = description;
    new->enc = enc;
    strncpy(new->mode, mode, 4);
    new->mode[4] = '\0';
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
    Rconnection new = (Rconnection) malloc(sizeof(struct Rconn));
    if (!new)
        error(_("allocation of %s connection failed"), class_name);

    SEXP addrObj = R_MakeExternalPtr(new, R_NilValue, R_NilValue);
    SEXP fastRConn = ((call_R_new_custom_connection) callbacks[R_new_custom_connection_x])(description, mode,
                                                                                           class_name, addrObj);
    // printf("DEBUG: R_new_custom_connection address %p SEXP value %p\n", ptr, addrObj);
    if (fastRConn) {
        new->class = (char *) malloc(strlen(class_name) + 1);
        if (!new->class) {
            free(new);
            error(_("allocation of %s connection failed"), class_name);
        }
        strcpy(new->class, class_name);
        new->description = (char *) malloc(strlen(description) + 1);
        if (!new->description) {
            free(new->class);
            free(new);
            error(_("allocation of %s connection failed"), class_name);
        }
        init_con(new, (char *) description, CE_NATIVE, mode);
        /* all ptrs are init'ed to null_* so no need to repeat that,
         but the following two are useful tools which could not be accessed otherwise */
        // TODO dummy_vfprintf and dummy_fgetc not implemented in FastR yet
        //    new->vfprintf = &dummy_vfprintf;
        //    new->fgetc = &dummy_fgetc;

        /* new->blocking = block; */
        new->encname[0] = 0; /* "" (should have the same effect as "native.enc") */
        new->ex_ptr = R_MakeExternalPtr(new->id, install("connection"), R_NilValue);

        SEXP class = allocVector(STRSXP, 2);
        SET_STRING_ELT(class, 0, mkChar(class_name));
        SET_STRING_ELT(class, 1, mkChar("connection"));
        classgets(fastRConn, class);
        // setAttrib(ans, R_ConnIdSymbol, new->ex_ptr); -- TODO not implemented/needed? in FastR

        if (ptr) {
            *ptr = new;
        }
    }

    return fastRConn;
}

/*
 * The address of the Rconnection struct is passed to Java.
 * Since down calls can only have Object parameters, we put the address into an int vector.
 * Position 0 is the lower part and position 1 is the higher part of the address.
 * This currently assumes max. 64-bit addresses !
 */
static Rconnection convertToAddress(SEXP addrObj) {
    if (!inherits(addrObj, "externalptr")) {
        error(_("invalid address object"));
    }
    return (Rconnection) R_ExternalPtrAddr(addrObj);

}

/* --------------------------------------------------------------------------- */
/* ------------------- Functions used as Java down calls --------------------- */

/* These functions are invoked from Java when the user does some operation on a
 * custom connection registered via R_new_custom_connection. We only have native
 * functions for such connection. These functions are invoked through the same
 * mechanism as e.g. the .C or .Call builtin, i.e. they accept only SEXP arguments
 * and may be either void or return SEXP. Otherwise we'd have to provide signature
 * of each function so that NFI knows how to convert the arguments and we'd have to
 * provide another mechanism to call native functions (aside .C/.Call/etc.)
 * with supplied signature. Therefore:
 *
 * DO NOT CHANGE SIGNATURE OF THESE FUNCTIONS!
 * If you do, update 'NativeConnections.java' accordingly.
 * Arguments and return type can ONLY BE SEXP.
 */

SEXP __GetFlagNativeConnection(SEXP rConnAddrObj, SEXP nameVec) {
    Rconnection con = convertToAddress(rConnAddrObj);
    const char *name = CHAR(Rf_asChar(nameVec));
    // printf("DEBUG: __GetFlagNativeConnection address %p SEXP value %p, flag '%s'\n", con, rConnAddrObj, name);
    if (strcmp(name, "text") == 0) {
        return ScalarLogical(con->text);
    } else if (strcmp(name, "isopen") == 0) {
        return ScalarLogical(con->isopen);
    } else if (strcmp(name, "incomplete") == 0) {
        return ScalarLogical(con->incomplete);
    } else if (strcmp(name, "canread") == 0) {
        return ScalarLogical(con->canread);
    } else if (strcmp(name, "canwrite") == 0) {
        return ScalarLogical(con->canwrite);
    } else if (strcmp(name, "canseek") == 0) {
        return ScalarLogical(con->canseek);
    } else if (strcmp(name, "blocking") == 0) {
        return ScalarLogical(con->blocking);
    }
    char errorBuffer[128];
    sprintf(errorBuffer, "Unknown flag '%.12s' in __GetFlagNativeConnection. "
            "This function should be used from NativeConnections.java", name);
    error(errorBuffer);
}

SEXP __OpenNativeConnection(SEXP rConnAddrObj) {
    Rconnection con = convertToAddress(rConnAddrObj);
    Rboolean success = con->open(con);
    return ScalarLogical(success);
}

void __CloseNativeConnection(SEXP rConnAddrObj) {
    Rconnection con = convertToAddress(rConnAddrObj);
    con->close(con);
}

// Note: we do not check if connection is open, this should be done on the Java side

SEXP __ReadNativeConnection(SEXP rConnAddrObj, SEXP bufVec, SEXP nVec) {
    Rconnection con = convertToAddress(rConnAddrObj);
    int n = asInteger(nVec);
    if (!con->canread) {
        error(_("cannot read from this connection"));
    }
    return ScalarInteger(con->read(RAW(bufVec), 1, n, con));
}

SEXP __WriteNativeConnection(SEXP rConnAddrObj, SEXP bufVec, SEXP nVec) {
    Rconnection con = convertToAddress(rConnAddrObj);
    int n = asInteger(nVec);
    if (!con->canread) {
        error(_("cannot read from this connection"));
    }
    return ScalarInteger(con->write(RAW(bufVec), 1, n, con));
}

SEXP __SeekNativeConnection(SEXP rConnAddrObj, SEXP whereVec, SEXP originVec, SEXP rwVec) {
    Rconnection con = convertToAddress(rConnAddrObj);
    int rw = asInteger(rwVec);
    int origin = asInteger(originVec);
    double where = asReal(whereVec);
    return ScalarReal(con->seek(con, where, origin, rw));
}

/* --------------------------------------------------------------------------- */
/* ---------------- R API functions for accessing connections ---------------- */

/* These functions are used by packages to read/write data from/to connections.
 * We have to upcall to Java, because the default connections are implemented there.
 * If the connection happens to be custom connection implemented on the native side,
 * then the Java side finds out and downcalls back to e.g. __ReadNativeConnection
 */

size_t R_ReadConnection(Rconnection con, void *buf, size_t n) {
    return (size_t) ((call_R_ReadConnection) callbacks[R_ReadConnection_x])(getFd(con), (long) buf, (int) n);
}

size_t R_WriteConnection(Rconnection con, void *buf, size_t n) {
    return (size_t) ((call_R_WriteConnection) callbacks[R_WriteConnection_x])(getFd(con), (long) buf, (int) n);
}

Rconnection R_GetConnection(SEXP sConn) {
    if (!inherits(sConn, "connection")) {
        error(_("invalid connection"));
    }

    int fd = asInteger(sConn);

    SEXP fastRCon = ((call_R_GetConnection) callbacks[R_GetConnection_x])(fd);
    char *connClass = ((call_getConnectionClassString) callbacks[getConnectionClassString_x])(fastRCon);
    char *summaryDesc = ((call_getSummaryDescription) callbacks[getSummaryDescription_x])(fastRCon);
    char *openMode = ((call_getOpenModeString) callbacks[getOpenModeString_x])(fastRCon);
    int isSeekable = ((call_isSeekable) callbacks[isSeekable_x])(fastRCon);

    Rconnection new = (Rconnection) malloc(sizeof(struct Rconn));
    if (!new) {
        error(_("allocation of file connection failed"));
    }

    init_con(new, summaryDesc, 0, openMode);
    free(openMode); // the init_con function makes a copy
    new->class = connClass;
    new->canseek = (Rboolean) isSeekable;
    setFd(new, fd);

    // TODO implement up-call functions and set them
    // In fact reasonable code should see Rconnection as opaque pointer and does not attempt
    // at calling these functions directly, but use e.g. R_WriteConnection instead. What is
    // however important is to update the flags (e.g. opened) according to the current state
    // on Java side -- i.e. writeLines may temporarily open-close the connection.
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

