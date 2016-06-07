#
# This material is distributed under the GNU General Public License
# Version 2. You may review the terms of this license at
# http://www.gnu.org/licenses/gpl-2.0.html
#
# Copyright (c) 1995, 1996  Robert Gentleman and Ross Ihaka
# Copyright (c) 1997-2013,  The R Core Team
# Copyright (c) 2016, Oracle and/or its affiliates
#
# All rights reserved.
#

#
# This file defines FastR counterparts of C function Cdqrls defined in lm.c in GnuR
# This functions are called using .Call. See model.R for details
#

Cdqrls <- function(x, y, tol, check) {
    ans <- dim(x)
    if (check && length(ans) != 2) {
        error("'x' is not a matrix")
    }
    n <- ans[[1]]
    p <- ans[[2]]
    ny <- if (n != 0L) ny <- length(y)%/%n else 0L;
    
    if (check && n * ny != length(y)) {
        error(paste0("dimensions of 'x' (",n ,"," ,p , ") and 'y' (", length(y), ") do not match"));
    }
    
    if (any(!is.finite(x))) {
        error("NA/NaN/Inf in 'x'")
    }
    if (any(!is.finite(y))) {
        error("NA/NaN/Inf in 'y'")
    }
    
    # GnuR allocates memory, assigns it to a list that will be later returned and with 
    # this allocated memory it invokes Fortran code to actually fill it in with useful 
    # data. We do not have to pre-allocate, so we just invoke Fortran
    coeff <- mat.or.vec(p, ny)
    storage.mode(coeff) <- 'double'
    storage.mode(tol) <- 'double'
    
    # in the call to dqrls we omit pointers to preallocated memory that does not 
    # hold any actual input data. These are allocated in Java wrapper for _fastr_dqrls
    # GnuR constucts resulting list from the pointers passed to Fortran, here we 
    # expect the Java wrapper to construct and return such list (i.e. the wrapper does 
    # some work that in GnuR is done here in Cdqrls)
    result <- .fastr.dqrls(x, n, p, y, ny, tol, coeff)
    
    result$pivoted <- 0
    for (i in 1:p) {
        if (result$pivot[i] != i) {
            result$pivoted <- 1
            break
        }
    }
    
    result
}


