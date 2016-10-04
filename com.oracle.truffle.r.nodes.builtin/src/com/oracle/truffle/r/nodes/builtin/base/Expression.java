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

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RPromise;

@RBuiltin(name = "expression", kind = PRIMITIVE, parameterNames = {"..."}, nonEvalArgs = 0, behavior = PURE)
public abstract class Expression extends RBuiltinNode {
    /*
     * Owing to the nonEvalArgs, all arguments are RPromise, but an expression may contain
     * non-RLanguage elements.
     */
    private final ConditionProfile isEvaluatedProfile = ConditionProfile.createBinaryProfile();

    @Specialization
    @ExplodeLoop
    protected Object doExpression(RArgsValuesAndNames args) {
        Object[] argValues = args.getArguments();
        Object[] data = new Object[argValues.length];
        ArgumentsSignature signature = args.getSignature();
        boolean hasNonNull = signature.getNonNullCount() > 0;
        for (int i = 0; i < argValues.length; i++) {
            data[i] = convert((RPromise) argValues[i]);
        }
        if (hasNonNull) {
            String[] names = new String[signature.getLength()];
            for (int i = 0; i < names.length; i++) {
                names[i] = signature.getName(i);
            }
            return RDataFactory.createExpression(data, RDataFactory.createStringVector(names, RDataFactory.COMPLETE_VECTOR));
        } else {
            return RDataFactory.createExpression(data);
        }
    }

    @Specialization
    protected Object doExpression(RPromise language) {
        return RDataFactory.createExpression(new Object[]{convert(language)});
    }

    private Object convert(RPromise promise) {
        if (isEvaluatedProfile.profile(promise.isEvaluated())) {
            return promise.getValue();
        } else {
            return RASTUtils.createLanguageElement(promise.getRep().asRSyntaxNode());
        }
    }
}
