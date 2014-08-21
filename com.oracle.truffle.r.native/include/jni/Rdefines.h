/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
#ifndef R_DEFINES_H
#define R_DEFINES_H

#include <Rinternals.h>

#define INTEGER_VALUE(x)  asInteger(x)
#define NEW_INTEGER(n)		allocVector(INTSXP,n)

#endif
