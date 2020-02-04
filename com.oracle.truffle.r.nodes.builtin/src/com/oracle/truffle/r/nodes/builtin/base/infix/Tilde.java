/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.builtin.base.infix;

import static com.oracle.truffle.r.runtime.builtins.RBehavior.READS_FRAME;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.attributes.GetFixedPropertyNode;
import com.oracle.truffle.r.nodes.attributes.SetFixedAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.SetClassAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.nodes.function.RCallNode.BuiltinCallNode;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.Closure;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RCodeBuilder;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * This a rather strange function. It is where, in GnuR, that the "formula" class is set and the
 * ".Environment" attribute on the "call". N.B. the "response" can be missing, which is actually
 * handled by an evaluated argument of type {@link RMissing}, although it appears as if the "model"
 * argument is missing, i.e. {@code ~ x} result in {@code `~`(x)}.
 */
@RBuiltin(name = "~", kind = PRIMITIVE, parameterNames = {"x", "y"}, nonEvalArgs = {0, 1}, lookupVarArgs = false, behavior = READS_FRAME)
public abstract class Tilde extends RBuiltinNode.Arg2 {

    private static final RStringVector FORMULA_CLASS = RDataFactory.createStringVectorFromScalar(RRuntime.FORMULA_CLASS);

    @Child private SetClassAttributeNode setClassAttrNode = SetClassAttributeNode.create();
    @Child private GetFixedPropertyNode getClassAttrNode;
    @Child private SetFixedAttributeNode setEnvAttrNode;

    static {
        Casts.noCasts(Tilde.class);
    }

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RMissing.instance, RMissing.instance};
    }

    protected SetFixedAttributeNode createSetEnvAttrNode() {
        return SetFixedAttributeNode.create(RRuntime.DOT_ENVIRONMENT);
    }

    @Specialization
    protected RPairList tilde(VirtualFrame frame, Object x, Object y,
                    @Cached("create()") BranchProfile callNodeAttrsProfile,
                    @Cached("create()") BranchProfile classDefinedProfile) {

        if (setEnvAttrNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            setEnvAttrNode = insert(createSetEnvAttrNode());
        }

        // The call syntax node constructed for the tilde builtin sometimes already contains the
        // class attribute extending the "formula" class with another class, such as "quosure" from
        // rlang. The call syntax node's class attribute must be propagated to the resulting
        // language object.
        BuiltinCallNode builtinCallNode = ((BuiltinCallNode) getParent());
        DynamicObject callAttrs = builtinCallNode.getRSyntaxNode().getAttributes();

        RCallNode call = createCall(x, y, callAttrs);

        // Do not cache the closure because formulas are usually not evaluated.
        RPairList lang = RDataFactory.createLanguage(Closure.createLanguageClosure(call));

        if (callAttrs != null) {
            callNodeAttrsProfile.enter();

            if (getClassAttrNode == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                getClassAttrNode = insert(GetFixedPropertyNode.createClass());
            }
            Object classAttr = getClassAttrNode.execute(callAttrs);
            if (classAttr != null) {
                classDefinedProfile.enter();
                return lang;
            }
        }

        setClassAttrNode.setAttr(lang, FORMULA_CLASS);
        REnvironment env = REnvironment.frameToEnvironment(frame.materialize());
        setEnvAttrNode.setAttr(lang, env);

        return lang;
    }

    @TruffleBoundary
    private static RCallNode createCall(Object response, Object model, DynamicObject callAttrs) {
        RCodeBuilder<RSyntaxNode> astBuilder = RContext.getASTBuilder();

        RSyntaxNode functionLookup = astBuilder.lookup(RSyntaxNode.LAZY_DEPARSE, "~", true);
        if (model == RMissing.instance) {
            return (RCallNode) astBuilder.call(RSyntaxNode.LAZY_DEPARSE, functionLookup, getRep(response), callAttrs);
        }
        return (RCallNode) astBuilder.call(RSyntaxNode.LAZY_DEPARSE, functionLookup, getRep(response), getRep(model), callAttrs);
    }

    private static RSyntaxNode getRep(Object o) {
        CompilerAsserts.neverPartOfCompilation();
        if (o instanceof RPromise) {
            return RContext.getRRuntimeASTAccess().unwrapPromiseRep((RPromise) o);
        }
        return RContext.getASTBuilder().constant(RSyntaxNode.LAZY_DEPARSE, o);
    }

}
