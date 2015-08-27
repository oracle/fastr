/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
#include <R.h>

void F77_NAME(xerbla)(const char *srname, int *info)
{
   /* srname is not null-terminated.  It should be 6 characters. */
    char buf[7];
    strncpy(buf, srname, 6);
    buf[6] = '\0';
    printf("BLAS/LAPACK routine '%6s' gave error code %d", buf, -(*info));
}
