/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.context;

import com.oracle.truffle.r.runtime.WithFunctionUID;
import com.oracle.truffle.r.runtime.data.RFunction;

public abstract class RInstrumentFactory {

    public abstract void registerFunctionDefinitionNode(WithFunctionUID fdn);

    public abstract Object findSingleProbe(RFunction func, Object tag);

    public abstract void checkDebugRequested(RFunction func);

    public abstract boolean enableDebug(RFunction func, Object text, Object condition, boolean once);

    public abstract boolean undebug(RFunction func);

    public abstract boolean isDebugged(RFunction func);

    public abstract boolean enableTrace(RFunction func);

    public abstract boolean disableTrace(RFunction func);

    public abstract void setTracingState(boolean state);

    public abstract boolean installCounter(RFunction func);

    public abstract int getCounter(RFunction func);

    public abstract boolean installFunctionTimer(RFunction func);

    public abstract long getFunctionTime(RFunction func);

}
