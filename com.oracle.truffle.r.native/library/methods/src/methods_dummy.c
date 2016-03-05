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

static void unimplemented_methods() {
	error("unimplemented methods .External");
}


SEXP R_M_setPrimitiveMethods(SEXP fname, SEXP op, SEXP code_vec,
			     SEXP fundef, SEXP mlist){ unimplemented_methods(); return NULL; }
SEXP R_clear_method_selection(){ unimplemented_methods(); return NULL; }
SEXP R_dummy_extern_place(){ unimplemented_methods(); return NULL; }
SEXP R_el_named(SEXP object, SEXP what){ unimplemented_methods(); return NULL; }
SEXP R_externalptr_prototype_object(){ unimplemented_methods(); return NULL; }
SEXP R_getGeneric(SEXP name, SEXP mustFind, SEXP env, SEXP package){ unimplemented_methods(); return NULL; }
SEXP R_get_slot(SEXP obj, SEXP name){ unimplemented_methods(); return NULL; }
SEXP R_getClassFromCache(SEXP class, SEXP table){ unimplemented_methods(); return NULL; }
SEXP R_hasSlot(SEXP obj, SEXP name){ unimplemented_methods(); return NULL; }
SEXP R_identC(SEXP e1, SEXP e2){ unimplemented_methods(); return NULL; }
SEXP R_initMethodDispatch(SEXP envir){ unimplemented_methods(); return NULL; }
SEXP R_methodsPackageMetaName(SEXP prefix, SEXP name, SEXP pkg){ unimplemented_methods(); return NULL; }
SEXP R_methods_test_MAKE_CLASS(SEXP className){ unimplemented_methods(); return NULL; }
SEXP R_methods_test_NEW(SEXP className){ unimplemented_methods(); return NULL; }
SEXP R_missingArg(SEXP symbol, SEXP ev){ unimplemented_methods(); return NULL; }
SEXP R_nextMethodCall(SEXP matched_call, SEXP ev){ unimplemented_methods(); return NULL; }
SEXP R_quick_method_check(SEXP args, SEXP mlist, SEXP fdef){ unimplemented_methods(); return NULL; }
SEXP R_selectMethod(SEXP fname, SEXP ev, SEXP mlist, SEXP evalArgs){ unimplemented_methods(); return NULL; }
SEXP R_set_el_named(SEXP object, SEXP what, SEXP value){ unimplemented_methods(); return NULL; }
SEXP R_set_slot(SEXP obj, SEXP name, SEXP value){ unimplemented_methods(); return NULL; }
SEXP R_standardGeneric(SEXP fname, SEXP ev, SEXP fdef){ unimplemented_methods(); return NULL; }
SEXP do_substitute_direct(SEXP f, SEXP env){ unimplemented_methods(); return NULL; }
SEXP Rf_allocS4Object(){ unimplemented_methods(); return NULL; }
SEXP R_set_method_dispatch(SEXP onOff){ unimplemented_methods(); return NULL; }
SEXP R_get_primname(SEXP object){ unimplemented_methods(); return NULL; }
SEXP new_object(SEXP class_def){ unimplemented_methods(); return NULL; }
