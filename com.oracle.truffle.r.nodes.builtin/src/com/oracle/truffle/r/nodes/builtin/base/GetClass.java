/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;

@RBuiltin(name = "class", kind = PRIMITIVE, parameterNames = {"x"})
public abstract class GetClass extends RBuiltinNode {

    @Specialization(guards = {"isObject", "!isLanguage", "!isExpression"})
    protected Object getClassForObject(RAbstractContainer arg) {
        controlVisibility();
        return arg.getClassHierarchy();
    }

    @Specialization(guards = {"!isObject", "!isLanguage", "!isExpression"})
    protected Object getClass(RAbstractContainer arg) {
        controlVisibility();
        final String klass = arg.getClassHierarchy().getDataAt(0);
        if (klass.equals(RType.Double.getName())) {
            return RType.Numeric.getName();
        }
        return klass;
    }

    @Specialization
    protected Object getClass(@SuppressWarnings("unused") RFunction arg) {
        controlVisibility();
        return RType.Function.getName();
    }

    @Specialization
    protected Object getClass(@SuppressWarnings("unused") RFormula arg) {
        controlVisibility();
        return RType.Formula.getName();
    }

    @Specialization
    protected Object getClass(@SuppressWarnings("unused") RNull arg) {
        controlVisibility();
        return RType.Null.getName();
    }

    @Specialization
    protected Object getClass(@SuppressWarnings("unused") RSymbol arg) {
        controlVisibility();
        return RRuntime.CLASS_SYMBOL;
    }

    @Specialization
    protected Object getClass(@SuppressWarnings("unused") REnvironment arg) {
        controlVisibility();
        return RType.Environment.getName();
    }

    @Specialization
    protected Object getClass(@SuppressWarnings("unused") RPairList arg) {
        controlVisibility();
        return RType.PairList.getName();
    }

    @Specialization
    protected Object getClass(@SuppressWarnings("unused") RLanguage arg) {
        controlVisibility();
        return RRuntime.CLASS_LANGUAGE;
    }

    @Specialization
    protected Object getClass(@SuppressWarnings("unused") RExpression arg) {
        controlVisibility();
        return RRuntime.CLASS_EXPRESSION;
    }

    @Specialization
    protected Object getClass(RConnection arg) {
        controlVisibility();
        return arg.getClassHierarchy();
    }

    protected boolean isExpression(RAbstractContainer arg) {
        return arg.getElementClass() == RExpression.class;
    }

    protected boolean isLanguage(RAbstractContainer arg) {
        return arg.getElementClass() == RLanguage.class;
    }

    protected boolean isObject(RAbstractContainer arg) {
        return arg.isObject();
    }

    public abstract Object execute(VirtualFrame frame, RAbstractVector o);
}
