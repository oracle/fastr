/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RExpression;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

@RBuiltin(name = "as.call", kind = PRIMITIVE, parameterNames = {"x"}, behavior = PURE)
public abstract class AsCall extends RBuiltinNode {

    private final ConditionProfile nullNamesProfile = ConditionProfile.createBinaryProfile();
    @Child private GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();

    static {
        Casts.noCasts(AsCall.class);
    }

    @Specialization
    protected RLanguage asCallFunction(RList x) {
        // TODO error checks
        RArgsValuesAndNames avn = makeNamesAndValues(x);
        if (x.getDataAt(0) instanceof RSymbol) {
            return Call.makeCallSourceUnavailable(((RSymbol) x.getDataAt(0)).getName(), avn);
        } else if (x.getDataAt(0) instanceof String) {
            return Call.makeCallSourceUnavailable((String) x.getDataAt(0), avn);
        } else if (x.getDataAt(0) instanceof RAbstractStringVector) {
            return Call.makeCallSourceUnavailable(((RAbstractStringVector) x.getDataAt(0)).getDataAt(0), avn);
        } else if (x.getDataAt(0) instanceof RFunction) {
            return Call.makeCallSourceUnavailable((RFunction) x.getDataAt(0), avn);
        } else {
            throw RInternalError.unimplemented();
        }
    }

    @Specialization
    protected RLanguage asCallFunction(RExpression x) {
        // TODO error checks
        String f;
        if (x.getDataAt(0) instanceof RSymbol) {
            f = ((RSymbol) x.getDataAt(0)).getName();
        } else {
            RLanguage l = (RLanguage) x.getDataAt(0);
            f = ((ReadVariableNode) RASTUtils.unwrap(l.getRep())).getIdentifier();
        }
        return Call.makeCallSourceUnavailable(f, makeNamesAndValues(x));
    }

    private RArgsValuesAndNames makeNamesAndValues(RAbstractContainer x) {
        int length = x.getLength() - 1;
        Object[] values = new Object[length];
        for (int i = 0; i < length; i++) {
            values[i] = x.getDataAtAsObject(i + 1);
        }
        ArgumentsSignature signature;
        if (nullNamesProfile.profile(getNamesNode.getNames(x) == null)) {
            signature = ArgumentsSignature.empty(values.length);
        } else {
            String[] names = new String[length];
            // extract names, converting "" to null
            RStringVector ns = getNamesNode.getNames(x);
            for (int i = 0; i < length; i++) {
                String name = ns.getDataAt(i + 1);
                if (name != null && !name.isEmpty()) {
                    names[i] = name;
                }
            }
            signature = ArgumentsSignature.get(names);
        }

        return new RArgsValuesAndNames(values, signature);
    }

    @Fallback
    protected Object asCallFunction(@SuppressWarnings("unused") Object x) {
        throw error(RError.Message.GENERIC, "invalid argument list");
    }
}
