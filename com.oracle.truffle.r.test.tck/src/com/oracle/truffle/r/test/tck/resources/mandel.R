# Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 3 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 3 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 3 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
mandel_fun = function(z) {
    c = z
    maxiter = 80
    for (n in 1:maxiter) {
        if (Mod(z) > 2) return(n-1)
        z = z^2+c
    }
    return(maxiter)
}

Mandel = function(x) {
    re = seq(-2,0.5,.1)
    im = seq(-1,1,.1)
    M = matrix(0.0,nrow=length(re),ncol=length(im))
    count = 1
    for (r in re) {
        for (i in im) {
            M[count] = mandel_fun(complex(real=r,imag=i))
            count = count + 1
        }
    }
    return(sum(M))
}

function() {
    return (Mandel(c()))
}
