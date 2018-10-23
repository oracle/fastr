# Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
stopifnot(require(testrffi))

charsxp1 <- api.STRING_ELT(c("a"), 0)
charsxp2 <- api.STRING_ELT(c("b"), 0)
charsxp3 <- api.STRING_ELT(c("c"), 0)
charsxp4 <- api.STRING_ELT(c("d"), 0)
charsxp5 <- api.STRING_ELT(c("e"), 0)
charsxp6 <- api.STRING_ELT(c("f"), 0)
charsxp7 <- api.STRING_ELT(c("g"), 0)
charsxp8 <- api.STRING_ELT(c("h"), 0)

a <- c(charsxp1, charsxp2, charsxp3, charsxp4, charsxp5, charsxp6, charsxp7, charsxp8)

duplicated(a)
duplicated(c(a, charsxp1, charsxp2))

identical(charsxp1, api.STRING_ELT(c("a"), 0))
identical(a, a)
identical(NULL, a)
identical(a, NULL)
identical(a, c(a, charsxp1))
identical(c(a, charsxp1), a)

unique(a)
unique(c(a, charsxp1, charsxp2))

match(c(charsxp1), c(charsxp1))
match(c(charsxp1), a)
match(a, c(charsxp1))
match(a, c(charsxp1, charsxp4))
match(a, c(charsxp2, charsxp1, charsxp4))

nzchar(c(a, charsxp1))