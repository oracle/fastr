/*
 * Copyright (c) 2014, 2014, Oracle and/or its affiliates. All rights reserved.
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

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

@RBuiltin(name = "expression", kind = PRIMITIVE, parameterNames = {"..."}, nonEvalArgs = {-1})
public abstract class Expression extends RBuiltinNode {
    /*
     * Owing to the nonEvalArgs, all arguments are RPromise, but an expression actually consists of
     * RLanguage elements so we convert, even though an RPromise is a subclass.
     */

    @Specialization
    public Object doExpression(Object[] args) {
        RLanguage[] data = new RLanguage[args.length];
        for (int i = 0; i < args.length; i++) {
            data[i] = convert((RPromise) args[i]);
        }
        RList list = RDataFactory.createList(data);
        return RDataFactory.createExpression(list);
    }

    @Specialization
    public Object doExpression(RPromise language) {
        RList list = RDataFactory.createList(new Object[]{convert(language)});
        return RDataFactory.createExpression(list);
    }

    private static RLanguage convert(RPromise promise) {
        return RDataFactory.createLanguage(promise.getRep());
    }

}
