/*
 * Copyright (c) 2015, 2022, Oracle and/or its affiliates. All rights reserved.
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
#define NO_FASTR_REDEFINE
#include <R.h>
#include <Rinternals.h>
#include <R_ext/Rdynload.h>
#include "testrffi.h"
#include "serialization.h"
#include "promises.h"
#include "charsxps.h"
#include "rapi_helpers.h"
#include "rffiwrappers.h"

static const R_CMethodDef CEntries[]  = {
    {"rapi_dotC", (DL_FUNC) &rapi_dotC, 3},
    {"dotCModifiedArguments", (DL_FUNC) &dotCModifiedArguments, 5},
    {"benchRf_isNull", (DL_FUNC) &benchRf_isNull, 1},
    {NULL, NULL, 0}
};

static const R_ExternalMethodDef ExternalEntries[]  = {
    {"rapi_dotExternal", (DL_FUNC) &rapi_dotExternal, 3},
    {"rapi_dotExternal2", (DL_FUNC) &rapi_dotExternal2, 3},
    {NULL, NULL, 0}
};

#define CALLDEF(name, n)  {#name, (DL_FUNC) &name, n}

/* It is not strictly necessary to include every C function in this table, as
 * it simply enables a call to be made with the "C_name" variable style of .Call,
 * as opposed to simply using the string "name". The latter is the default mechanism
 * used in testrffi.c.
*/

static const R_CallMethodDef CallEntries[] = {
        CALLDEF(addInt, 2),
        CALLDEF(addDouble, 2),
        CALLDEF(populateIntVector, 1),
        CALLDEF(populateDoubleVector, 1),
        CALLDEF(populateComplexVector, 1),
        CALLDEF(populateCharacterVector, 1),
        CALLDEF(populateRawVector, 1),
        CALLDEF(populateLogicalVector, 1),
        CALLDEF(createExternalPtr, 3),
        CALLDEF(getExternalPtrAddr, 1),
        CALLDEF(invoke_TYPEOF, 1),
        CALLDEF(invoke_error, 1),
        CALLDEF(dot_external_access_args, 1),
        CALLDEF(invoke_isString, 1),
        CALLDEF(invoke_fun, 3),
        CALLDEF(invoke12, 12),
        CALLDEF(interactive, 0),
        CALLDEF(tryEval, 2),
        CALLDEF(rHomeDir, 0),
        CALLDEF(nestedCall1, 2),
        CALLDEF(nestedCall2, 1),
        CALLDEF(r_home, 0),
        CALLDEF(mkStringFromChar, 0),
        CALLDEF(mkStringFromBytes, 0),
        CALLDEF(mkStringFromRaw, 0),
        CALLDEF(null, 0),
        CALLDEF(iterate_iarray, 1),
        CALLDEF(iterate_iptr, 1),
        CALLDEF(preserve_object, 1),
        CALLDEF(release_object, 1),
        CALLDEF(findvar, 2),
		CALLDEF(shareIntElement, 4),
		CALLDEF(shareDoubleElement, 4),
		CALLDEF(shareListElement, 4),
		CALLDEF(shareStringElement, 4),
        CALLDEF(test_asReal, 1),
        CALLDEF(test_asChar, 1),
        CALLDEF(test_asInteger, 1),
        CALLDEF(test_asLogical, 1),
        CALLDEF(test_CAR, 1),
        CALLDEF(test_CDR, 1),
        CALLDEF(test_LENGTH, 1),
        CALLDEF(test_inlined_length, 1),
        CALLDEF(test_coerceVector, 2),
        CALLDEF(test_ATTRIB, 1),
        CALLDEF(test_stringNA, 0),
        CALLDEF(test_captureDotsWithSingleElement, 1),
        CALLDEF(test_evalAndNativeArrays, 3),
        CALLDEF(test_writeConnection, 1),
        CALLDEF(test_readConnection, 1),
        CALLDEF(test_createNativeConnection, 0),
        CALLDEF(test_ParseVector, 1),
        CALLDEF(test_RfFindFunAndRfEval, 2),
        CALLDEF(test_lapply, 3),
        CALLDEF(test_RfEvalWithPromiseInPairList, 0),
        CALLDEF(test_isNAString, 1),
        CALLDEF(test_getBytes, 1),
        CALLDEF(test_setStringElt, 2),
        CALLDEF(test_RfRandomFunctions, 0),
        CALLDEF(test_RfRMultinom, 0),
        CALLDEF(test_RfFunctions, 0),
        CALLDEF(test_DATAPTR, 2),
        CALLDEF(test_duplicate, 2),
        CALLDEF(test_R_nchar, 1),
        CALLDEF(test_forceAndCall, 3),
        CALLDEF(test_constant_types, 0),
        CALLDEF(test_sort_complex, 1),
        CALLDEF(testMultiSetProtection, 0),
        CALLDEF(get_dataptr, 1),
        CALLDEF(benchMultipleUpcalls, 1),
        CALLDEF(benchProtect, 2),
        CALLDEF(test_lapplyWithForceAndCall, 4),
        CALLDEF(rapi_dotCall, 2),
        CALLDEF(testMissingArgWithATTRIB, 0),
        CALLDEF(testPRIMFUN, 2),
        CALLDEF(serialize, 1),
        CALLDEF(testInstallTrChar, 2),
        CALLDEF(charsxp_replace_nth_str, 3),
        CALLDEF(charsxp_nth_str, 2),
        CALLDEF(charsxp_create_empty_str, 1),
        CALLDEF(charsxp_revert_via_elt, 1),
        CALLDEF(charsxp_revert_via_dataptr, 1),
        CALLDEF(charsxp_tests, 0),
        CALLDEF(promises_create_promise, 2),
        CALLDEF(promises_put_into_env, 2),
        CALLDEF(promises_tests, 0),
        #include "init_api.h"
        {NULL, NULL, 0}
};

void
R_init_testrffi(DllInfo *dll)
{
    R_registerRoutines(dll, CEntries, CallEntries, NULL, ExternalEntries);
}
