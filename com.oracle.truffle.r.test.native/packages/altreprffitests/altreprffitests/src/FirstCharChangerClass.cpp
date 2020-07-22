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
#include "FirstCharChangerClass.hpp"


SEXP FirstCharChangerClass::createInstance(SEXP char_vec, SEXP replace_char)
{
    if (TYPEOF(char_vec) != STRSXP || TYPEOF(replace_char) != STRSXP ||
        LENGTH(replace_char) > 1)
    {
        Rf_error("Wrong parameters");
    }

    R_altrep_class_t descr = R_make_altstring_class("FirstCharChanger", "altreprffitests", nullptr);
    R_set_altrep_Length_method(descr, &Length);
    R_set_altvec_Dataptr_method(descr, &Dataptr);
    R_set_altstring_Elt_method(descr, &Elt);
    R_set_altstring_Set_elt_method(descr, &Set_elt);

    return R_new_altrep(descr, char_vec, STRING_ELT(replace_char, 0));
}

R_xlen_t FirstCharChangerClass::Length(SEXP instance)
{
    return LENGTH(R_altrep_data1(instance));
}

void * FirstCharChangerClass::Dataptr(SEXP instance, Rboolean writeabble)
{
    return DATAPTR(R_altrep_data1(instance));
}

SEXP FirstCharChangerClass::Elt(SEXP instance, R_xlen_t idx)
{
    SEXP char_vec = R_altrep_data1(instance);
    SEXP replace_char = R_altrep_data2(instance);
    if (TYPEOF(replace_char) != CHARSXP) {
        Rf_error("Internal error in Elt");
    }

    const char *old_string = Rf_translateChar(STRING_ELT(char_vec, idx));
    char *new_string = static_cast<char *>( std::malloc( std::strlen(old_string)));
    std::strcpy(new_string, old_string);
    new_string[0] = Rf_translateChar(replace_char)[0];
    SEXP new_elem = Rf_mkChar(new_string);
    // TODO: Valid here?
    std::free(new_string);
    return new_elem;
}

void FirstCharChangerClass::Set_elt(SEXP instance, R_xlen_t idx, SEXP elem)
{
    Rf_error("Should not reach here");
}
