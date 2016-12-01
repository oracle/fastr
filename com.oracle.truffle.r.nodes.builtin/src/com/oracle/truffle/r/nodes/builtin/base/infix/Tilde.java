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
package com.oracle.truffle.r.nodes.builtin.base.infix;

import static com.oracle.truffle.r.runtime.builtins.RBehavior.READS_FRAME;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node.Child;
import com.oracle.truffle.r.nodes.attributes.SetClassAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * This a rather strange function. It is where, in GnuR, that the "formula" class is set and the
 * ".Environment" attribute on the "call". N.B. the "response" can be missing, which is actually
 * handled by an evaluated argument of type {@link RMissing}, although it appears as if the "model"
 * argument is missing, i.e. {@code ~ x} result in {@code `~`(x)}.
 */
@RBuiltin(name = "~", kind = PRIMITIVE, parameterNames = {"x", "y"}, nonEvalArgs = {0, 1}, behavior = READS_FRAME)
public abstract class Tilde extends RBuiltinNode {

    private static final RStringVector FORMULA_CLASS = RDataFactory.createStringVectorFromScalar(RRuntime.FORMULA_CLASS);

    @Child private SetClassAttributeNode setClassAttrNode = SetClassAttributeNode.create();

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RMissing.instance, RMissing.instance};
    }

    @Specialization
    protected RLanguage tilde(VirtualFrame frame, @SuppressWarnings("unused") Object response, @SuppressWarnings("unused") Object model) {
        RCallNode call = (RCallNode) ((RBaseNode) getParent()).asRSyntaxNode();
        RLanguage lang = RDataFactory.createLanguage(call);
        setClassAttrNode.execute(lang, FORMULA_CLASS);
        REnvironment env = REnvironment.frameToEnvironment(frame.materialize());
        lang.setAttr(RRuntime.DOT_ENVIRONMENT, env);
        return lang;
    }
}
