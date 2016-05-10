/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.CastStringNodeGen;
import com.oracle.truffle.r.runtime.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractVector;

@RBuiltin(name = "names<-", kind = PRIMITIVE, parameterNames = {"x", "value"})
public abstract class UpdateNames extends RBuiltinNode {

    @Child private CastStringNode castStringNode;

    private Object castString(Object o) {
        if (castStringNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castStringNode = insert(CastStringNodeGen.create(false, true, false, false));
        }
        return castStringNode.executeString(o);
    }

    public abstract Object executeStringVector(RAbstractContainer container, Object o);

    @Specialization
    @TruffleBoundary
    protected RAbstractContainer updateNames(RAbstractContainer container, Object names) {
        controlVisibility();
        Object newNames = castString(names);
        if (newNames == RNull.instance) {
            RAbstractContainer result = (RAbstractContainer) container.getNonShared();
            result.setNames(null);
            return result;
        }

        RStringVector stringVector;
        if (newNames instanceof String) {
            stringVector = RDataFactory.createStringVector((String) newNames);
        } else {
            stringVector = (RStringVector) ((RAbstractVector) newNames).materialize();
        }
        RAbstractContainer result = (RAbstractContainer) container.getNonShared();
        if (stringVector.getLength() < result.getLength()) {
            stringVector = stringVector.copyResized(result.getLength(), true);
        } else if (stringVector == container) {
            stringVector = (RStringVector) stringVector.copy();
        }
        result.setNames(stringVector);
        return result;
    }
}
