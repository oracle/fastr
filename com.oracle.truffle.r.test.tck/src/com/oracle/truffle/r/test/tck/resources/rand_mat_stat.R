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
RandMatStat = function(t) {
    set.seed(10)
    n = 5
    v = matrix(0, nrow=t)
    w = matrix(0, nrow=t)
    for (i in 1:t) {
        a = matrix(rnorm(n*n), ncol=n, nrow=n)
        b = matrix(rnorm(n*n), ncol=n, nrow=n)
        c = matrix(rnorm(n*n), ncol=n, nrow=n)
        d = matrix(rnorm(n*n), ncol=n, nrow=n)
        P = cbind(a,b,c,d)
        Q = rbind(cbind(a,b),cbind(c,d))
        v[i] = sum(diag((t(P)%*%P)^4))
        w[i] = sum(diag((t(Q)%*%Q)^4))
    }
    s1 = apply(v,2,sd)/mean(v)
    s2 = apply(w,2,sd)/mean(w)
    return (round(s1, digits=7) == 0.8324299 && round(s2, digits=7) == 0.7440433)
}

function() {
    return (RandMatStat(1000))
}
