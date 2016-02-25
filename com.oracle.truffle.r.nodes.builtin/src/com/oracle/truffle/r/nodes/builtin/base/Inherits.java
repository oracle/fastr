/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 2014, Purdue University
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */

package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.unary.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.conn.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;

@RBuiltin(name = "inherits", kind = INTERNAL, parameterNames = {"x", "what", "which"})
// TODO inherits is applicable to every type of object, if only because of "try-error".
public abstract class Inherits extends RBuiltinNode {

    protected final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

    public abstract Object execute(Object x, Object what, Object which);

    @Child private InheritsNode inheritsNode;
    @Child private Inherits recursiveInherits;

    private InheritsNode initInheritsNode() {
        if (inheritsNode == null) {
            inheritsNode = insert(com.oracle.truffle.r.nodes.unary.InheritsNodeGen.create());
        }
        return inheritsNode;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object doesInherit(RNull x, RAbstractStringVector what, byte which) {
        return RRuntime.LOGICAL_FALSE;
    }

    @Specialization
    protected Object doesInherit(REnvironment x, RAbstractStringVector what, byte which) {
        return initInheritsNode().executeObject(x, what, which);
    }

    @Specialization
    protected Object doesInherit(RFunction x, RAbstractStringVector what, byte which) {
        return initInheritsNode().executeObject(x, what, which);
    }

    @Specialization
    protected Object doesInherit(RSymbol x, RAbstractStringVector what, byte which) {
        return initInheritsNode().executeObject(x, what, which);
    }

    @Specialization
    protected Object doesInherit(RConnection x, RAbstractStringVector what, byte which) {
        return initInheritsNode().executeObject(x, what, which);
    }

    @Specialization
    protected Object doesInherit(RAbstractContainer x, RAbstractStringVector what, byte which) {
        return initInheritsNode().executeObject(x, what, which);
    }

    @Specialization
    protected Object doesInherit(RArgsValuesAndNames x, RAbstractStringVector what, byte which) {
        assert x.getLength() == 1;
        if (recursiveInherits == null) {
            recursiveInherits = insert(com.oracle.truffle.r.nodes.builtin.base.InheritsNodeGen.create(null, null, null));
        }
        return recursiveInherits.execute(x.getArgument(0), what, which);
    }

    @Specialization
    protected Object doesInherit(RS4Object x, RAbstractStringVector what, byte which) {
        return initInheritsNode().executeObject(x, what, which);
    }

    @Specialization
    protected Object doesInherit(RExternalPtr x, RAbstractStringVector what, byte which) {
        return initInheritsNode().executeObject(x, what, which);
    }

}
