/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
#include "serialization.h"
#include <stdlib.h>
#include <string.h>

static void out_char(R_outpstream_t stream, int n);
static void out_bytes(R_outpstream_t stream, void *buffer, int n);

#define DATA_CAPACITY 2048

struct stream_data {
    char *data;
    int len;
};

/**
* Returns raw vector of serialized data.
*/
SEXP serialize(SEXP object) {
    struct stream_data sdata = {
        .data = (char *) malloc(DATA_CAPACITY),
        .len = 0
    };

    struct R_outpstream_st stream = {
        .data = &sdata,
        .type = R_pstream_xdr_format,
        .version = 3,
        .OutChar = &out_char,
        .OutBytes = &out_bytes,
        .OutPersistHookFunc = NULL,
        .OutPersistHookData = R_NilValue
    };

    R_Serialize(object, &stream);
    // Copy data to vector
    SEXP raw_vec = PROTECT(allocVector(RAWSXP, sdata.len));
    memcpy(RAW(raw_vec), sdata.data, sdata.len);
    free(sdata.data);

    UNPROTECT(1);
    return raw_vec;
}

static void out_bytes(R_outpstream_t stream, void *buffer, int n) {
    struct stream_data *sdata = (struct stream_data *) stream->data;
    if (sdata->len + n > DATA_CAPACITY) {
        error("Serialized data overflowed preallocated space");
    }
    int buff_idx = 0;
    for (int data_idx = sdata->len; data_idx < sdata->len + n; data_idx++) {
        sdata->data[data_idx] = ((char *)buffer) [buff_idx];
        buff_idx++;
    }
    sdata->len += n;
}

static void out_char(R_outpstream_t stream, int n)
{
    struct stream_data *sdata = (struct stream_data *) stream->data;
    if (sdata->len + 1 > DATA_CAPACITY) {
        error("Serialized data overflowed preallocated space");
    }
    sdata->data[sdata->len] = n;
    sdata->len++;
}
