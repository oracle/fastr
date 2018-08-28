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
# test access to global environment with multiple context instantiations varying context number

if (any(R.version$engine == "FastR")) {
    ch1 <- .fastr.channel.create(1L)
    code <- "ch2 <- .fastr.channel.get(1L); x <- .fastr.channel.receive(ch2); .fastr.channel.send(ch2, x)"
    x <- 7
    # create one child context
    cx <- .fastr.context.spawn(code)
    .fastr.channel.send(ch1, 7)
    y <- .fastr.channel.receive(ch1)
    .fastr.context.join(cx)
    # create two child contexts
    cx <- .fastr.context.spawn(rep(code, 2))
    .fastr.channel.send(ch1, 42)
    .fastr.channel.send(ch1, 24)
    y <- .fastr.channel.receive(ch1)
    z <- .fastr.channel.receive(ch1)
    
    .fastr.channel.close(ch1)
    print(sort(c(y, z)))
} else {
    print(c(24L, 42L))
}
