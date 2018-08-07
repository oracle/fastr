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
if (any(R.version$engine == "FastR")) {
    ch0 <- .fastr.channel.create(1L)
    ch1 <- .fastr.channel.create(2L)
    code0 <- "ch <- .fastr.channel.get(1L); .fastr.channel.send(ch, 7L)"
    code1 <- "ch <- .fastr.channel.get(2L); .fastr.channel.send(ch, 42L)"
    cx0 <- .fastr.context.spawn(code0)
    cx1 <- .fastr.context.spawn(code1)
    x<-.fastr.channel.receive(ch0)
    y<-.fastr.channel.receive(ch1)
    .fastr.context.join(cx0)
    .fastr.context.join(cx1)
    .fastr.channel.close(ch0)
    .fastr.channel.close(ch1)
    print(c(x,y))
} else {
    print(c(7L, 42L))
}
