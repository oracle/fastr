/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.test.altrep;

import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.r.runtime.data.NativeDataAccess;
import com.oracle.truffle.r.runtime.data.RBaseObject;
import com.oracle.truffle.r.runtime.data.RIntVector;
import org.junit.Assert;

import java.util.Arrays;

@ExportLibrary(InteropLibrary.class)
abstract class NativeFunctionMock implements TruffleObject {
    private boolean called;

    @ExportMessage
    public boolean isExecutable() {
        return true;
    }

    @ExportMessage
    Object execute(Object[] args) {
        Assert.assertTrue(args.length >= 1);
        assertInstanceParameter(args[0]);
        RIntVector instance = unwrapInstanceParameter(args[0]);
        setCalled();
        Object[] argsWithoutFirstArg = Arrays.copyOfRange(args, 1, args.length);
        return doExecute(instance, argsWithoutFirstArg);
    }

    private void assertInstanceParameter(Object instanceParam) {
        Assert.assertTrue(instanceParam instanceof NativeDataAccess.NativeMirror);
        RBaseObject mirroredObject = ((NativeDataAccess.NativeMirror) instanceParam).getDelegate();
        Assert.assertTrue(mirroredObject instanceof RIntVector);
    }

    private RIntVector unwrapInstanceParameter(Object instanceParam) {
        RBaseObject delegate = ((NativeDataAccess.NativeMirror) instanceParam).getDelegate();
        assert delegate instanceof RIntVector;
        return (RIntVector) delegate;
    }

    protected abstract Object doExecute(RIntVector instance, Object... args);

    public boolean wasCalled() {
        return called;
    }

    private void setCalled() {
        called = true;
    }
}
