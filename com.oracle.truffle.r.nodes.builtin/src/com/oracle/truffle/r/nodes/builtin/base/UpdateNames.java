/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

@RBuiltin(name = "names<-", kind = PRIMITIVE, parameterNames = {"x", ""})
// 2nd parameter is "value", but should not be matched against, so ""
@GenerateNodeFactory
public abstract class UpdateNames extends RInvisibleBuiltinNode {

    @Child private CastStringNode castStringNode;

    private Object castString(VirtualFrame frame, Object o) {
        if (castStringNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castStringNode = insert(CastStringNodeGen.create(null, false, true, false, false));
        }
        return castStringNode.executeString(frame, o);
    }

    public abstract Object executeStringVector(VirtualFrame frame, RAbstractContainer container, Object o);

    @Specialization
    @TruffleBoundary
    protected RAbstractContainer updateNames(RAbstractContainer container, @SuppressWarnings("unused") RNull names) {
        controlVisibility();
        RAbstractContainer result = container.materializeNonShared();
        result.setNames(null);
        return result;
    }

    @Specialization
    @TruffleBoundary
    protected RAbstractContainer updateNames(RAbstractContainer container, RStringVector names) {
        controlVisibility();
        RAbstractContainer result = container.materializeNonShared();
        RStringVector namesVector = names;
        if (names.getLength() < result.getLength()) {
            namesVector = names.copyResized(result.getLength(), true);
        }
        result.setNames(namesVector);
        return result;
    }

    @Specialization
    @TruffleBoundary
    protected RAbstractContainer updateNames(RAbstractContainer container, String name) {
        controlVisibility();
        RAbstractContainer result = container.materializeNonShared();
        String[] names = new String[result.getLength()];
        Arrays.fill(names, RRuntime.STRING_NA);
        names[0] = name;
        RStringVector namesVector = RDataFactory.createStringVector(names, names.length <= 1);
        result.setNames(namesVector);
        return result;
    }

    @Specialization
    protected RAbstractContainer updateNames(VirtualFrame frame, RAbstractContainer container, Object names) {
        controlVisibility();
        if (names instanceof RAbstractVector) {
            return updateNames(container, (RStringVector) castString(frame, names));
        } else {
            return updateNames(container, (String) castString(frame, names));
        }
    }

}
