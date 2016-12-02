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

import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetClassAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.CastStringNodeGen;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RString;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

// oldClass<- (as opposed to class<-), simply sets the attribute (without handling "implicit" attributes)
@RBuiltin(name = "oldClass<-", kind = PRIMITIVE, parameterNames = {"x", "value"}, behavior = PURE)
public abstract class UpdateOldClass extends RBuiltinNode {

    @Child private CastStringNode castStringNode;
    @Child private SetClassAttributeNode setClassAttributeNode = SetClassAttributeNode.create();

    @Specialization(guards = "!isStringVector(className)")
    protected Object setOldClass(RAbstractContainer arg, RAbstractVector className) {
        if (className.getLength() == 0) {
            return setOldClass(arg, RNull.instance);
        }
        initCastStringNode();
        Object result = castStringNode.execute(className);
        return setOldClass(arg, (RStringVector) result);
    }

    private void initCastStringNode() {
        if (castStringNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castStringNode = insert(CastStringNodeGen.create(false, false, false));
        }
    }

    @Specialization
    @TruffleBoundary
    protected Object setOldClass(RAbstractContainer arg, String className) {
        return setOldClass(arg, RDataFactory.createStringVector(className));
    }

    @Specialization
    @TruffleBoundary
    protected Object setOldClass(RAbstractContainer arg, RStringVector className) {
        RAbstractContainer result = (RAbstractContainer) arg.getNonShared();
        setClassAttributeNode.execute(result, className);
        return result;
    }

    @Specialization
    @TruffleBoundary
    protected Object setOldClass(RAbstractContainer arg, @SuppressWarnings("unused") RNull className) {
        RAbstractContainer result = (RAbstractContainer) arg.getNonShared();
        setClassAttributeNode.reset(result);
        return result;
    }

    protected boolean isStringVector(RAbstractVector className) {
        return className.getElementClass() == RString.class;
    }
}
