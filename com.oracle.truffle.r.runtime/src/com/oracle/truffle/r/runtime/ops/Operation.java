/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.ops;

import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ReturnException;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

public class Operation extends RBaseNode {

    private final boolean commutative;
    private final boolean associative;

    public Operation(boolean commutative, boolean associative) {
        this.commutative = commutative;
        this.associative = associative;
    }

    public boolean isCommutative() {
        return commutative;
    }

    public boolean isAssociative() {
        return associative;
    }

    public static RuntimeException handleException(Throwable e) {
        if (e instanceof RError) {
            throw (RError) e;
        } else if (e instanceof ReturnException) {
            throw (ReturnException) e;
        } else {
            throw RInternalError.shouldNotReachHere(e, "only RErrors or ReturnExceptions should be thrown by arithmetic ops");
        }
    }
}
