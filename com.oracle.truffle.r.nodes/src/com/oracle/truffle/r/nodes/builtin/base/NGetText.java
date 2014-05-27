/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.nodes.unary.ConvertNode.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@SuppressWarnings("unused")
@RBuiltin(name = "ngettext", kind = INTERNAL)
public abstract class NGetText extends RBuiltinNode {

    @CreateCast("arguments")
    public RNode[] createCastValue(RNode[] children) {
        return new RNode[]{CastIntegerNodeFactory.create(children[0], false, false, false), children[1], children[2], children[3]};
    }

    @Specialization(order = 1, guards = "wrongNVector")
    public String getTextEmpty(RAbstractIntVector nVector, String msg1, String msg2, Object domain) {
        CompilerDirectives.transferToInterpreter();
        throw RError.getInvalidArgument(getEncapsulatingSourceSection(), "n");
    }

    @Specialization(order = 2, guards = "!wrongNVector")
    public String getText(RAbstractIntVector nVector, String msg1, String msg2, Object domain) {
        int n = nVector.getDataAt(0);
        return n == 1 ? msg1 : msg2;
    }

    @Specialization(order = 10, guards = "!wrongNVector")
    public String getTextMsg1Null(RAbstractIntVector nVector, RNull msg1, RNull msg2, Object domain) {
        CompilerDirectives.transferToInterpreter();
        throw RError.getMustBeString(getEncapsulatingSourceSection(), "msg1");
    }

    @Specialization(order = 11, guards = "!wrongNVector")
    public String getTextMsg1Null(RAbstractIntVector nVector, RNull msg1, RAbstractVector msg2, Object domain) {
        CompilerDirectives.transferToInterpreter();
        throw RError.getMustBeString(getEncapsulatingSourceSection(), "msg1");
    }

    @Specialization(order = 12, guards = {"!wrongNVector", "!msg1StringVectorOneElem"})
    public String getTextMsg1WrongMsg2Null(RAbstractIntVector nVector, RAbstractVector msg1, RNull msg2, Object domain) {
        CompilerDirectives.transferToInterpreter();
        throw RError.getMustBeString(getEncapsulatingSourceSection(), "msg1");
    }

    @Specialization(order = 13, guards = {"!wrongNVector", "!msg1StringVectorOneElem"})
    public String getTextMsg1Wrong(RAbstractIntVector nVector, RAbstractVector msg1, RAbstractVector msg2, Object domain) {
        CompilerDirectives.transferToInterpreter();
        throw RError.getMustBeString(getEncapsulatingSourceSection(), "msg1");
    }

    @Specialization(order = 20, guards = {"!wrongNVector", "msg1StringVectorOneElem"})
    public String getTextMsg1(RAbstractIntVector nVector, RAbstractVector msg1, RNull msg2, Object domain) {
        CompilerDirectives.transferToInterpreter();
        throw RError.getMustBeString(getEncapsulatingSourceSection(), "msg2");
    }

    @Specialization(order = 21, guards = {"!wrongNVector", "msg1StringVectorOneElem", "!msg2StringVectorOneElem"})
    public String getTextMsg2Wrong(RAbstractIntVector nVector, RAbstractVector msg1, RAbstractVector msg2, Object domain) {
        CompilerDirectives.transferToInterpreter();
        throw RError.getMustBeString(getEncapsulatingSourceSection(), "msg2");
    }

    @Specialization(order = 30, guards = {"!wrongNVector", "msg1StringVectorOneElem", "msg2StringVectorOneElem"})
    public String getTextMsg1(RAbstractIntVector nVector, RAbstractVector msg1, RAbstractVector msg2, Object domain) {
        return getText(nVector, ((RAbstractStringVector) msg1).getDataAt(0), ((RAbstractStringVector) msg2).getDataAt(0), domain);
    }

    protected boolean wrongNVector(RAbstractIntVector nVector) {
        return nVector.getLength() == 0 || (nVector.getLength() > 0 && nVector.getDataAt(0) < 0);
    }

    protected boolean msg1StringVectorOneElem(RAbstractIntVector nVector, RAbstractVector msg1) {
        return msg1.getElementClass() == RString.class && msg1.getLength() == 1;
    }

    protected boolean msg2StringVectorOneElem(RAbstractIntVector nVector, RAbstractVector msg1, RAbstractVector msg2) {
        return msg2.getElementClass() == RString.class && msg2.getLength() == 1;
    }
}
