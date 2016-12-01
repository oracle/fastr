/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.builtins.RBehavior.PURE;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.attributes.SetClassAttributeNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.unary.CastStringNode;
import com.oracle.truffle.r.nodes.unary.CastStringNodeGen;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;

@RBuiltin(name = "formatC", kind = INTERNAL, parameterNames = {"x", "mode", "width", "digits", "format", "flag", "i.strlen"}, behavior = PURE)
public abstract class FormatC extends RBuiltinNode {

    @Child private CastStringNode castStringNode;

    private RStringVector castStringVector(Object o) {
        if (castStringNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            castStringNode = insert(CastStringNodeGen.create(true, true, true));
        }
        return (RStringVector) ((RStringVector) castStringNode.executeString(o)).copyDropAttributes();
    }

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg("x");
        casts.arg("mode").asStringVector().findFirst();
        casts.arg("width").asIntegerVector().findFirst();
        casts.arg("digits").asIntegerVector().findFirst();
        casts.arg("format").asStringVector().findFirst();
        casts.arg("flag").asStringVector().findFirst();
        casts.arg("i.strlen").asIntegerVector().findFirst();
    }

    @SuppressWarnings("unused")
    @Specialization
    RAttributable formatC(RAbstractContainer x, String mode, int width, int digits, String format, String flag, int iStrlen,
                    @Cached("create()") SetClassAttributeNode setClassAttrNode) {
        RStringVector res = castStringVector(x);
        setClassAttrNode.reset(res);
        return res;
    }
}
