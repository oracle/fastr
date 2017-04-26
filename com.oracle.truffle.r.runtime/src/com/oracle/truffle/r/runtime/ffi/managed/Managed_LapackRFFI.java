/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi.managed;

import static com.oracle.truffle.r.runtime.ffi.managed.Managed_RFFIFactory.unsupported;

import com.oracle.truffle.r.runtime.ffi.LapackRFFI;

public class Managed_LapackRFFI implements LapackRFFI {
    @Override
    public IlaverNode createIlaverNode() {
        throw unsupported("lapack");
    }

    @Override
    public DgeevNode createDgeevNode() {
        throw unsupported("lapack");
    }

    @Override
    public Dgeqp3Node createDgeqp3Node() {
        throw unsupported("lapack");
    }

    @Override
    public DormqrNode createDormqrNode() {
        throw unsupported("lapack");
    }

    @Override
    public DtrtrsNode createDtrtrsNode() {
        throw unsupported("lapack");
    }

    @Override
    public DgetrfNode createDgetrfNode() {
        throw unsupported("lapack");
    }

    @Override
    public DpotrfNode createDpotrfNode() {
        throw unsupported("lapack");
    }

    @Override
    public DpotriNode createDpotriNode() {
        throw unsupported("lapack");
    }

    @Override
    public DpstrfNode createDpstrfNode() {
        throw unsupported("lapack");
    }

    @Override
    public DgesvNode createDgesvNode() {
        throw unsupported("lapack");
    }

    @Override
    public DlangeNode createDlangeNode() {
        throw unsupported("lapack");
    }

    @Override
    public DgeconNode createDgeconNode() {
        throw unsupported("lapack");
    }

    @Override
    public DsyevrNode createDsyevrNode() {
        throw unsupported("lapack");
    }
}
