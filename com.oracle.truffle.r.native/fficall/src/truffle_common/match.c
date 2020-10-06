/*
 * Copyright (c) 2017, 2020, Oracle and/or its affiliates. All rights reserved.
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
#include "rffi_upcalls.h"

Rboolean Rf_NonNullStringMatch(SEXP s, SEXP t)
{
	return ((call_Rf_NonNullStringMatch) callbacks[Rf_NonNullStringMatch_x])(s, t);
}

SEXP Rf_matchE(SEXP itable, SEXP ix, int nmatch, SEXP env)
{
	return ((call_match5) callbacks[match5_x])(itable, ix, nmatch, NULL, env);
}

SEXP Rf_match(SEXP itable, SEXP ix, int nmatch)
{
	return ((call_match5) callbacks[match5_x])(itable, ix, nmatch, NULL, R_BaseEnv);
}

SEXP match5(SEXP itable, SEXP ix, int nmatch, SEXP incomparables, SEXP env)
{
	return ((call_match5) callbacks[match5_x])(itable, ix, nmatch, incomparables, env);
}
