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
        if (klass.equals(RRuntime.TYPE_DOUBLE)) {
            return RRuntime.TYPE_NUMERIC;
        }
        return klass;
    }

    @Specialization
    protected Object getClass(@SuppressWarnings("unused") RFunction arg) {
        controlVisibility();
        return RRuntime.TYPE_FUNCTION;
    }

    @Specialization
    protected Object getClass(@SuppressWarnings("unused") RFormula arg) {
        controlVisibility();
        return RRuntime.TYPE_FORMULA;
    }

    @Specialization
    protected Object getClass(@SuppressWarnings("unused") RNull arg) {
        controlVisibility();
        return RRuntime.NULL;
    }

    @Specialization
    protected Object getClass(@SuppressWarnings("unused") RSymbol arg) {
        controlVisibility();
        return RRuntime.CLASS_SYMBOL;
    }

    @Specialization
    protected Object getClass(@SuppressWarnings("unused") REnvironment arg) {
        controlVisibility();
        return RRuntime.TYPE_ENVIRONMENT;
    }

    @Specialization
    protected Object getClass(@SuppressWarnings("unused") RPairList arg) {
        controlVisibility();
        return RRuntime.TYPE_PAIR_LIST;
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
