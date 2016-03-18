/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.engine.interop;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.interop.ForeignAccess.Factory10;
import com.oracle.truffle.api.interop.Message;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.runtime.RInternalError;

public final class RFunctionAccessFactory implements Factory10 {

    @Override
    public CallTarget accessIsNull() {
        throw RInternalError.shouldNotReachHere("message: accessIsNull");
    }

    @Override
    public CallTarget accessIsExecutable() {
        return Truffle.getRuntime().createCallTarget(new ConstantRootNode(ConstantNode.create(true)));
    }

    @Override
    public CallTarget accessIsBoxed() {
        throw RInternalError.shouldNotReachHere("message: accessIsBoxed");
    }

    @Override
    public CallTarget accessHasSize() {
        return Truffle.getRuntime().createCallTarget(new ConstantRootNode(ConstantNode.create(false)));
    }

    @Override
    public CallTarget accessGetSize() {
        throw RInternalError.shouldNotReachHere("message: accessIsBoxed");
    }

    @Override
    public CallTarget accessUnbox() {
        throw RInternalError.shouldNotReachHere("message: accessUnbox");
    }

    @Override
    public CallTarget accessRead() {
        throw RInternalError.shouldNotReachHere("message: accessIsBoxed");
    }

    @Override
    public CallTarget accessWrite() {
        throw RInternalError.shouldNotReachHere("message: accessWrite");
    }

    @Override
    public CallTarget accessExecute(int argumentsLength) {
        return Truffle.getRuntime().createCallTarget(new RInteropExecuteNode(argumentsLength));
    }

    @Override
    public CallTarget accessInvoke(int argumentsLength) {
        return Truffle.getRuntime().createCallTarget(new RInteropExecuteNode(argumentsLength));
    }

    @Override
    public CallTarget accessMessage(Message unknown) {
        throw RInternalError.shouldNotReachHere("message: " + unknown);
    }

    @Override
    @SuppressWarnings("all")
    public CallTarget accessNew(int argumentsLength) {
        return null;
    }
}
