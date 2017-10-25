/*
 *  R : A Computer Language for Statistical Data Analysis
 *  Copyright (C) 2005-12   The R Core Team.
 *
 *  This program is free software{ return NULL; } you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation{ return NULL; } either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY{ return NULL; } without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program{ return NULL; } if not, a copy is available at
 *  http://www.r-project.org/Licenses/
 */

#include <Rinternals.h>

#define UNIMPLEMENTED { error("unimplemented function at %s:%d", __FILE__, __LINE__); return NULL; }


SEXP R_M_setPrimitiveMethods(SEXP fname, SEXP op, SEXP code_vec,
			     SEXP fundef, SEXP mlist) UNIMPLEMENTED
SEXP R_clear_method_selection() UNIMPLEMENTED
SEXP R_dummy_extern_place() UNIMPLEMENTED
SEXP R_el_named(SEXP object, SEXP what) UNIMPLEMENTED
SEXP R_externalptr_prototype_object() UNIMPLEMENTED
SEXP R_getGeneric(SEXP name, SEXP mustFind, SEXP env, SEXP package) UNIMPLEMENTED
SEXP R_get_slot(SEXP obj, SEXP name) UNIMPLEMENTED
SEXP R_getClassFromCache(SEXP class, SEXP table) UNIMPLEMENTED
SEXP R_hasSlot(SEXP obj, SEXP name) UNIMPLEMENTED
SEXP R_identC(SEXP e1, SEXP e2) UNIMPLEMENTED
SEXP R_initMethodDispatch(SEXP envir) UNIMPLEMENTED
SEXP R_methodsPackageMetaName(SEXP prefix, SEXP name, SEXP pkg) UNIMPLEMENTED
SEXP R_methods_test_MAKE_CLASS(SEXP className) UNIMPLEMENTED
SEXP R_methods_test_NEW(SEXP className) UNIMPLEMENTED
SEXP R_missingArg(SEXP symbol, SEXP ev) UNIMPLEMENTED
SEXP R_nextMethodCall(SEXP matched_call, SEXP ev) UNIMPLEMENTED
SEXP R_quick_method_check(SEXP args, SEXP mlist, SEXP fdef) UNIMPLEMENTED
SEXP R_selectMethod(SEXP fname, SEXP ev, SEXP mlist, SEXP evalArgs) UNIMPLEMENTED
SEXP R_set_el_named(SEXP object, SEXP what, SEXP value) UNIMPLEMENTED
SEXP R_set_slot(SEXP obj, SEXP name, SEXP value) UNIMPLEMENTED
SEXP R_standardGeneric(SEXP fname, SEXP ev, SEXP fdef) UNIMPLEMENTED
SEXP do_substitute_direct(SEXP f, SEXP env) UNIMPLEMENTED
SEXP R_set_method_dispatch(SEXP onOff) UNIMPLEMENTED
SEXP R_get_primname(SEXP object) UNIMPLEMENTED
SEXP new_object(SEXP class_def) UNIMPLEMENTED
