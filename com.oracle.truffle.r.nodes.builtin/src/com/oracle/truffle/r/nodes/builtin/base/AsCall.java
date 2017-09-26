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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.RCallBaseNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractListBaseVector;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

@RBuiltin(name = "as.call", kind = PRIMITIVE, parameterNames = {"x"}, behavior = PURE)
public abstract class AsCall extends RBuiltinNode.Arg1 {

    private final ConditionProfile nullNamesProfile = ConditionProfile.createBinaryProfile();
    @Child private GetNamesAttributeNode getNamesNode = GetNamesAttributeNode.create();

    static {
        Casts.noCasts(AsCall.class);
    }

    @Specialization
    @TruffleBoundary
    protected RLanguage asCallFunction(RAbstractListBaseVector x) {
        if (x.getLength() == 0) {
            error(Message.INVALID_LEN_0_ARG);
        }
        // separate the first element (call target) from the rest (arguments)

        RSyntaxNode target = RASTUtils.createNodeForValue(x.getDataAt(0)).asRSyntaxNode();
        ArgumentsSignature signature = createSignature(x);
        Object[] arguments = new Object[signature.getLength()];
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = x.getDataAt(i + 1);
        }
        return Call.makeCall(getRLanguage(), target, arguments, signature);
    }

    private ArgumentsSignature createSignature(RAbstractContainer x) {
        int length = x.getLength() - 1;
        if (nullNamesProfile.profile(getNamesNode.getNames(x) == null)) {
            return ArgumentsSignature.empty(length);
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
            return ArgumentsSignature.get(names);
        }
    }

    protected boolean containsCall(RLanguage l) {
        return l.getRep() instanceof RCallBaseNode;
    }

    @Specialization(guards = "containsCall(l)")
    protected RLanguage asCall(RLanguage l) {
        return l;
    }

    @Fallback
    protected Object asCallFunction(@SuppressWarnings("unused") Object x) {
        throw error(RError.Message.GENERIC, "invalid argument list");
    }
}
