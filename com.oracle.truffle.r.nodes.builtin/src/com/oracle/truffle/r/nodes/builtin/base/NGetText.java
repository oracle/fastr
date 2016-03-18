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
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RString;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@SuppressWarnings("unused")
@RBuiltin(name = "ngettext", kind = INTERNAL, parameterNames = {"n", "msg1", "msg2", "domain"})
public abstract class NGetText extends RBuiltinNode {

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.toInteger(0);
    }

    @Specialization(guards = "wrongNVector(nVector)")
    protected String getTextEmpty(RAbstractIntVector nVector, String msg1, String msg2, Object domain) {
        throw RError.error(this, RError.Message.INVALID_ARGUMENT, "n");
    }

    @Specialization(guards = "!wrongNVector(nVector)")
    protected String getText(RAbstractIntVector nVector, String msg1, String msg2, Object domain) {
        int n = nVector.getDataAt(0);
        return n == 1 ? msg1 : msg2;
    }

    @Specialization(guards = "!wrongNVector(nVector)")
    protected String getTextMsg1Null(RAbstractIntVector nVector, RNull msg1, RNull msg2, Object domain) {
        throw RError.error(this, RError.Message.MUST_BE_STRING, "msg1");
    }

    @Specialization(guards = "!wrongNVector(nVector)")
    protected String getTextMsg1Null(RAbstractIntVector nVector, RNull msg1, RAbstractVector msg2, Object domain) {
        throw RError.error(this, RError.Message.MUST_BE_STRING, "msg1");
    }

    @Specialization(guards = {"!wrongNVector(nVector)", "!msgStringVectorOneElem(msg1)"})
    protected String getTextMsg1WrongMsg2Null(RAbstractIntVector nVector, RAbstractVector msg1, RNull msg2, Object domain) {
        throw RError.error(this, RError.Message.MUST_BE_STRING, "msg1");
    }

    @Specialization(guards = {"!wrongNVector(nVector)", "!msgStringVectorOneElem(msg1)"})
    protected String getTextMsg1Wrong(RAbstractIntVector nVector, RAbstractVector msg1, RAbstractVector msg2, Object domain) {
        throw RError.error(this, RError.Message.MUST_BE_STRING, "msg1");
    }

    @Specialization(guards = {"!wrongNVector(nVector)", "msgStringVectorOneElem(msg1)"})
    protected String getTextMsg1(RAbstractIntVector nVector, RAbstractVector msg1, RNull msg2, Object domain) {
        throw RError.error(this, RError.Message.MUST_BE_STRING, "msg2");
    }

    @Specialization(guards = {"!wrongNVector(nVector)", "msgStringVectorOneElem(msg1)", "!msgStringVectorOneElem(msg2)"})
    protected String getTextMsg2Wrong(RAbstractIntVector nVector, RAbstractVector msg1, RAbstractVector msg2, Object domain) {
        throw RError.error(this, RError.Message.MUST_BE_STRING, "msg2");
    }

    @Specialization(guards = {"!wrongNVector(nVector)", "msgStringVectorOneElem(msg1)", "msgStringVectorOneElem(msg2)"})
    protected String getTextMsg1(RAbstractIntVector nVector, RAbstractVector msg1, RAbstractVector msg2, Object domain) {
        return getText(nVector, ((RAbstractStringVector) msg1).getDataAt(0), ((RAbstractStringVector) msg2).getDataAt(0), domain);
    }

    protected boolean wrongNVector(RAbstractIntVector nVector) {
        return nVector.getLength() == 0 || (nVector.getLength() > 0 && nVector.getDataAt(0) < 0);
    }

    protected boolean msgStringVectorOneElem(RAbstractVector msg1) {
        return msg1.getElementClass() == RString.class && msg1.getLength() == 1;
    }
}
