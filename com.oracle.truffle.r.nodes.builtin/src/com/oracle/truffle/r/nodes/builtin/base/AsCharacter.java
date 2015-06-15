/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@RBuiltin(name = "as.character", kind = PRIMITIVE, parameterNames = {"x", "..."})
public abstract class AsCharacter extends RBuiltinNode {

    private static final String NAME = "as.character";

    @Child private CastStringNode castStringNode;
    @Child private UseMethodInternalNode dcn;
    private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    public abstract Object execute(VirtualFrame frame, Object obj);

    private void initCast() {
        if (castStringNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castStringNode = insert(CastStringNodeGen.create(false, false, false, false));
        }
    }

    private String castString(int o) {
        initCast();
        return (String) castStringNode.executeString(o);
    }

    private String castString(double o) {
        initCast();
        return (String) castStringNode.executeString(o);
    }

    private String castString(byte o) {
        initCast();
        return (String) castStringNode.executeString(o);
    }

    private RStringVector castStringVector(Object o) {
        initCast();
        return (RStringVector) ((RStringVector) castStringNode.executeString(o)).copyDropAttributes();
    }

    @Specialization
    protected String doInt(int value) {
        controlVisibility();
        return castString(value);
    }

    @Specialization
    protected String doDouble(double value) {
        controlVisibility();
        return castString(value);
    }

    @Specialization
    protected String doLogical(byte value) {
        controlVisibility();
        return castString(value);
    }

    @Specialization
    protected String doRaw(RRaw value) {
        controlVisibility();
        initCast();
        return (String) castStringNode.executeString(value);
    }

    @Specialization
    protected String doString(String value) {
        controlVisibility();
        return value;
    }

    @Specialization
    protected String doSymbol(RSymbol value) {
        controlVisibility();
        return value.getName();
    }

    @Specialization
    protected RStringVector doNull(@SuppressWarnings("unused") RNull value) {
        controlVisibility();
        return RDataFactory.createStringVector(0);
    }

    @Specialization(guards = "!isObject(frame, vector)")
    protected RStringVector doStringVector(@SuppressWarnings("unused") VirtualFrame frame, RStringVector vector) {
        controlVisibility();
        return RDataFactory.createStringVector(vector.getDataCopy(), vector.isComplete());
    }

    @Specialization(guards = "!isObject(frame, list)")
    protected RStringVector doList(@SuppressWarnings("unused") VirtualFrame frame, RList list) {
        controlVisibility();
        int len = list.getLength();
        boolean complete = RDataFactory.COMPLETE_VECTOR;
        String[] data = new String[len];
        for (int i = 0; i < len; i++) {
            Object elem = list.getDataAt(i);
            if (elem instanceof String) {
                data[i] = (String) elem;
            } else if (elem instanceof RStringVector && ((RStringVector) elem).getLength() == 1) {
                data[i] = ((RStringVector) elem).getDataAt(0);
            } else {
                data[i] = RDeparse.deparse1Line(elem, false);
            }
            if (RRuntime.isNA(data[i])) {
                complete = RDataFactory.INCOMPLETE_VECTOR;
            }
        }
        return RDataFactory.createStringVector(data, complete);
    }

    @Specialization(guards = "!isObject(frame, container)")
    protected RStringVector doVector(@SuppressWarnings("unused") VirtualFrame frame, RAbstractContainer container) {
        controlVisibility();
        return castStringVector(container);
    }

    @Specialization(guards = "isObject(frame, container)")
    protected Object doObject(VirtualFrame frame, RAbstractContainer container) {
        controlVisibility();
        if (dcn == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            dcn = insert(new UseMethodInternalNode(NAME, ArgumentsSignature.get(""), true));
        }
        try {
            return dcn.execute(frame, container, new Object[]{container});
        } catch (S3FunctionLookupNode.NoGenericMethodException e) {
            return castStringVector(container);
        }
    }

    @SuppressFBWarnings(value = "ES_COMPARING_STRINGS_WITH_EQ", justification = "generic name is interned in the interpreted code for faster comparison")
    protected boolean isObject(VirtualFrame frame, RAbstractContainer container) {
        return container.isObject(attrProfiles) && !(RArguments.getS3Args(frame) != null && RArguments.getS3Args(frame).generic == NAME);
    }
}
