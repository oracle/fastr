/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

#include <Rinternals.h>
#include <R_ext/Altrep.h>
#include "rffi_upcalls.h"

int (ALTREP)(SEXP x) {
    return ((call_ALTREP) callbacks[ALTREP_x])(x);
}

Rboolean R_altrep_inherits(SEXP instance, R_altrep_class_t class_descriptor) {
    return ((call_R_altrep_inherits) callbacks[R_altrep_inherits_x])(instance, class_descriptor);
}

SEXP R_altrep_data1(SEXP x) {
    return ((call_R_altrep_data1) callbacks[R_altrep_data1_x])(x);
}

SEXP R_altrep_data2(SEXP x) {
    return ((call_R_altrep_data2) callbacks[R_altrep_data2_x])(x);
}

void R_set_altrep_data1(SEXP instance, SEXP data1) {
    ((call_R_set_altrep_data1) callbacks[R_set_altrep_data1_x])(instance, data1);
}

void R_set_altrep_data2(SEXP instance, SEXP data2) {
    ((call_R_set_altrep_data2) callbacks[R_set_altrep_data2_x])(instance, data2);
}

/**** Altrep method setters ****/
void R_set_altrep_UnserializeEX_method(R_altrep_class_t class_descriptor, R_altrep_UnserializeEX_method_t method) {
    ((call_R_set_altrep_UnserializeEX_method) callbacks[R_set_altrep_UnserializeEX_method_x])(class_descriptor, method);
}

void R_set_altrep_Unserialize_method(R_altrep_class_t class_descriptor, R_altrep_Unserialize_method_t method) {
    ((call_R_set_altrep_Unserialize_method) callbacks[R_set_altrep_Unserialize_method_x])(class_descriptor, method);
}

void R_set_altrep_Serialized_state_method(R_altrep_class_t class_descriptor, R_altrep_Serialized_state_method_t method) {
    ((call_R_set_altrep_Serialized_state_method) callbacks[R_set_altrep_Serialized_state_method_x])(class_descriptor, method);
}

void R_set_altrep_Duplicate_method(R_altrep_class_t class_descriptor, R_altrep_Duplicate_method_t method) {
    ((call_R_set_altrep_Duplicate_method) callbacks[R_set_altrep_Duplicate_method_x])(class_descriptor, method);
}

void R_set_altrep_DuplicateEX_method(R_altrep_class_t class_descriptor, R_altrep_DuplicateEX_method_t method) {
    ((call_R_set_altrep_DuplicateEX_method) callbacks[R_set_altrep_DuplicateEX_method_x])(class_descriptor, method);
}

void R_set_altrep_Coerce_method(R_altrep_class_t class_descriptor, R_altrep_Coerce_method_t method) {
    ((call_R_set_altrep_Coerce_method) callbacks[R_set_altrep_Coerce_method_x])(class_descriptor, method);
}

void R_set_altrep_Inspect_method(R_altrep_class_t class_descriptor, R_altrep_Inspect_method_t method) {
    ((call_R_set_altrep_Inspect_method) callbacks[R_set_altrep_Inspect_method_x])(class_descriptor, method);
}

void R_set_altrep_Length_method(R_altrep_class_t class_descriptor, R_altrep_Length_method_t method) {
    ((call_R_set_altrep_Length_method) callbacks[R_set_altrep_Length_method_x])(class_descriptor, method);
}

/**** Altvec method setters ****/
void R_set_altvec_Dataptr_method(R_altrep_class_t class_descriptor, R_altvec_Dataptr_method_t method) {
    ((call_R_set_altvec_Dataptr_method) callbacks[R_set_altvec_Dataptr_method_x])(class_descriptor, method);
}

void R_set_altvec_Dataptr_or_null_method(R_altrep_class_t class_descriptor, R_altvec_Dataptr_or_null_method_t method) {
    ((call_R_set_altvec_Dataptr_or_null_method) callbacks[R_set_altvec_Dataptr_or_null_method_x])(class_descriptor, method);
}

void R_set_altvec_Extract_subset_method(R_altrep_class_t class_descriptor, R_altvec_Extract_subset_method_t method) {
    ((call_R_set_altvec_Extract_subset_method) callbacks[R_set_altvec_Extract_subset_method_x])(class_descriptor, method);
}

/**** Altinteger method setters ****/
void R_set_altinteger_Elt_method(R_altrep_class_t class_descriptor, R_altinteger_Elt_method_t method) {
    ((call_R_set_altinteger_Elt_method) callbacks[R_set_altinteger_Elt_method_x])(class_descriptor, method);
}

void R_set_altinteger_Get_region_method(R_altrep_class_t class_descriptor, R_altinteger_Get_region_method_t method) {
    ((call_R_set_altinteger_Get_region_method) callbacks[R_set_altinteger_Get_region_method_x])(class_descriptor, method);
}

void R_set_altinteger_Is_sorted_method(R_altrep_class_t class_descriptor, R_altinteger_Is_sorted_method_t method) {
    ((call_R_set_altinteger_Is_sorted_method) callbacks[R_set_altinteger_Is_sorted_method_x])(class_descriptor, method);
}

void R_set_altinteger_No_NA_method(R_altrep_class_t class_descriptor, R_altinteger_No_NA_method_t method) {
    ((call_R_set_altinteger_No_NA_method) callbacks[R_set_altinteger_No_NA_method_x])(class_descriptor, method);
}

void R_set_altinteger_Sum_method(R_altrep_class_t class_descriptor, R_altinteger_Sum_method_t method) {
    ((call_R_set_altinteger_Sum_method) callbacks[R_set_altinteger_Sum_method_x])(class_descriptor, method);
}

void R_set_altinteger_Min_method(R_altrep_class_t class_descriptor, R_altinteger_Min_method_t method) {
    ((call_R_set_altinteger_Min_method) callbacks[R_set_altinteger_Min_method_x])(class_descriptor, method);
}

void R_set_altinteger_Max_method(R_altrep_class_t class_descriptor, R_altinteger_Max_method_t method) {
    ((call_R_set_altinteger_Max_method) callbacks[R_set_altinteger_Max_method_x])(class_descriptor, method);
}

/**** Altreal method setters ****/
void R_set_altreal_Elt_method(R_altrep_class_t class_descriptor, R_altreal_Elt_method_t method) {
    ((call_R_set_altreal_Elt_method) callbacks[R_set_altreal_Elt_method_x])(class_descriptor, method);
}

void R_set_altreal_Get_region_method(R_altrep_class_t class_descriptor, R_altreal_Get_region_method_t method) {
    ((call_R_set_altreal_Get_region_method) callbacks[R_set_altreal_Get_region_method_x])(class_descriptor, method);
}

void R_set_altreal_Is_sorted_method(R_altrep_class_t class_descriptor, R_altreal_Is_sorted_method_t method) {
    ((call_R_set_altreal_Is_sorted_method) callbacks[R_set_altreal_Is_sorted_method_x])(class_descriptor, method);
}

void R_set_altreal_No_NA_method(R_altrep_class_t class_descriptor, R_altreal_No_NA_method_t method) {
    ((call_R_set_altreal_No_NA_method) callbacks[R_set_altreal_No_NA_method_x])(class_descriptor, method);
}

void R_set_altreal_Sum_method(R_altrep_class_t class_descriptor, R_altreal_Sum_method_t method) {
    ((call_R_set_altreal_Sum_method) callbacks[R_set_altreal_Sum_method_x])(class_descriptor, method);
}

void R_set_altreal_Min_method(R_altrep_class_t class_descriptor, R_altreal_Min_method_t method) {
    ((call_R_set_altreal_Min_method) callbacks[R_set_altreal_Min_method_x])(class_descriptor, method);
}

void R_set_altreal_Max_method(R_altrep_class_t class_descriptor, R_altreal_Max_method_t method) {
    ((call_R_set_altreal_Max_method) callbacks[R_set_altreal_Max_method_x])(class_descriptor, method);
}

/**** Altlogical method setters ****/
void R_set_altlogical_Elt_method(R_altrep_class_t class_descriptor, R_altlogical_Elt_method_t method) {
    ((call_R_set_altlogical_Elt_method) callbacks[R_set_altlogical_Elt_method_x])(class_descriptor, method);
}

void R_set_altlogical_Get_region_method(R_altrep_class_t class_descriptor, R_altlogical_Get_region_method_t method) {
    ((call_R_set_altlogical_Get_region_method) callbacks[R_set_altlogical_Get_region_method_x])(class_descriptor, method);
}

void R_set_altlogical_Is_sorted_method(R_altrep_class_t class_descriptor, R_altlogical_Is_sorted_method_t method) {
    ((call_R_set_altlogical_Is_sorted_method) callbacks[R_set_altlogical_Is_sorted_method_x])(class_descriptor, method);
}

void R_set_altlogical_No_NA_method(R_altrep_class_t class_descriptor, R_altlogical_No_NA_method_t method) {
    ((call_R_set_altlogical_No_NA_method) callbacks[R_set_altlogical_No_NA_method_x])(class_descriptor, method);
}

void R_set_altlogical_Sum_method(R_altrep_class_t class_descriptor, R_altlogical_Sum_method_t method) {
    ((call_R_set_altlogical_Sum_method) callbacks[R_set_altlogical_Sum_method_x])(class_descriptor, method);
}


/**** Altraw method setters ****/
void R_set_altraw_Elt_method(R_altrep_class_t class_descriptor, R_altraw_Elt_method_t method) {
    ((call_R_set_altraw_Elt_method) callbacks[R_set_altraw_Elt_method_x])(class_descriptor, method);
}

void R_set_altraw_Get_region_method(R_altrep_class_t class_descriptor, R_altraw_Get_region_method_t method) {
    ((call_R_set_altraw_Get_region_method) callbacks[R_set_altraw_Get_region_method_x])(class_descriptor, method);
}

/**** Altcomplex method setters ****/
void R_set_altcomplex_Elt_method(R_altrep_class_t class_descriptor, R_altcomplex_Elt_method_t method) {
    ((call_R_set_altcomplex_Elt_method) callbacks[R_set_altcomplex_Elt_method_x])(class_descriptor, method);
}

void R_set_altcomplex_Get_region_method(R_altrep_class_t class_descriptor, R_altcomplex_Get_region_method_t method) {
    ((call_R_set_altcomplex_Get_region_method) callbacks[R_set_altcomplex_Get_region_method_x])(class_descriptor, method);
}

/**** Altstring method setters ****/
void R_set_altstring_Elt_method(R_altrep_class_t class_descriptor, R_altstring_Elt_method_t method) {
    ((call_R_set_altstring_Elt_method) callbacks[R_set_altstring_Elt_method_x])(class_descriptor, method);
}

void R_set_altstring_Set_elt_method(R_altrep_class_t class_descriptor, R_altstring_Set_elt_method_t method) {
    ((call_R_set_altstring_Set_elt_method) callbacks[R_set_altstring_Set_elt_method_x])(class_descriptor, method);
}

void R_set_altstring_Is_sorted_method(R_altrep_class_t class_descriptor, R_altstring_Is_sorted_method_t method) {
    ((call_R_set_altstring_Is_sorted_method) callbacks[R_set_altstring_Is_sorted_method_x])(class_descriptor, method);
}

void R_set_altstring_No_NA_method(R_altrep_class_t class_descriptor, R_altstring_No_NA_method_t method) {
    ((call_R_set_altstring_No_NA_method) callbacks[R_set_altstring_No_NA_method_x])(class_descriptor, method);
}


R_altrep_class_t R_make_altinteger_class(const char *cname, const char *pname, DllInfo *info) {
    return ((call_R_make_altinteger_class) callbacks[R_make_altinteger_class_x])(cname, pname, info);
}

R_altrep_class_t R_make_altreal_class(const char *cname, const char *pname, DllInfo *info) {
    return ((call_R_make_altreal_class) callbacks[R_make_altreal_class_x])(cname, pname, info);
}

R_altrep_class_t R_make_altcomplex_class(const char *cname, const char *pname, DllInfo *info) {
    return ((call_R_make_altcomplex_class) callbacks[R_make_altcomplex_class_x])(cname, pname, info);
}

R_altrep_class_t R_make_altlogical_class(const char *cname, const char *pname, DllInfo *info) {
    return ((call_R_make_altlogical_class) callbacks[R_make_altlogical_class_x])(cname, pname, info);
}

R_altrep_class_t R_make_altstring_class(const char *cname, const char *pname, DllInfo *info) {
    return ((call_R_make_altstring_class) callbacks[R_make_altstring_class_x])(cname, pname, info);
}

R_altrep_class_t R_make_altraw_class(const char *cname, const char *pname, DllInfo *info) {
    return ((call_R_make_altraw_class) callbacks[R_make_altraw_class_x])(cname, pname, info);
}

SEXP R_new_altrep(R_altrep_class_t aclass, SEXP data1, SEXP data2) {
    SEXP class_desc = R_SEXP(aclass);
    SEXP res = ((call_R_new_altrep) callbacks[R_new_altrep_x])(class_desc, data1, data2);
    return res;
}

R_xlen_t INTEGER_GET_REGION(SEXP x, R_xlen_t start_idx, R_xlen_t size, int *buf) {
    return ((call_INTEGER_GET_REGION) callbacks[INTEGER_GET_REGION_x])(x, start_idx, size, buf);
}

R_xlen_t REAL_GET_REGION(SEXP x, R_xlen_t start_idx, R_xlen_t size, double *buf) {
    return ((call_REAL_GET_REGION) callbacks[REAL_GET_REGION_x])(x, start_idx, size, buf);
}

R_xlen_t LOGICAL_GET_REGION(SEXP x, R_xlen_t start_idx, R_xlen_t size, int *buf) {
    return ((call_LOGICAL_GET_REGION) callbacks[LOGICAL_GET_REGION_x])(x, start_idx, size, buf);
}

R_xlen_t COMPLEX_GET_REGION(SEXP x, R_xlen_t start_idx, R_xlen_t size, Rcomplex *buf) {
    return ((call_COMPLEX_GET_REGION) callbacks[COMPLEX_GET_REGION_x])(x, start_idx, size, buf);
}

R_xlen_t RAW_GET_REGION(SEXP x, R_xlen_t start_idx, R_xlen_t size, Rbyte *buf) {
    return ((call_RAW_GET_REGION) callbacks[RAW_GET_REGION_x])(x, start_idx, size, buf);
}

int INTEGER_NO_NA(SEXP x) {
    return ((call_INTEGER_NO_NA) callbacks[INTEGER_NO_NA_x])(x);
}

int REAL_NO_NA(SEXP x) {
    return ((call_REAL_NO_NA) callbacks[REAL_NO_NA_x])(x);
}

int LOGICAL_NO_NA(SEXP x) {
    return ((call_LOGICAL_NO_NA) callbacks[LOGICAL_NO_NA_x])(x);
}

int STRING_NO_NA(SEXP x) {
    return ((call_STRING_NO_NA) callbacks[STRING_NO_NA_x])(x);
}

int INTEGER_IS_SORTED(SEXP x) {
    return ((call_INTEGER_IS_SORTED) callbacks[INTEGER_IS_SORTED_x])(x);
}

int REAL_IS_SORTED(SEXP x) {
    return ((call_REAL_IS_SORTED) callbacks[REAL_IS_SORTED_x])(x);
}

int LOGICAL_IS_SORTED(SEXP x) {
    return ((call_LOGICAL_IS_SORTED) callbacks[LOGICAL_IS_SORTED_x])(x);
}

int STRING_IS_SORTED(SEXP x) {
    return ((call_STRING_IS_SORTED) callbacks[STRING_IS_SORTED_x])(x);
}
