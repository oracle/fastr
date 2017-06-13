/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (C) 2001-3 Paul Murrell
 * Copyright (c) 1998-2013, The R Core Team
 * Copyright (c) 2017, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.library.fastrGrid;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.library.fastrGrid.ViewPort.InitViewPortNode;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RNull;

final class LInitViewPortStack extends RExternalBuiltinNode {
    @Child private InitViewPortNode initViewPortNode = new InitViewPortNode();
    static {
        Casts.noCasts(LInitViewPortStack.class);
    }

    @Override
    public Object call(VirtualFrame frame, RArgsValuesAndNames args) {
        initViewPortNode.execute(frame);
        return RNull.instance;
    }

    @Override
    protected Object call(RArgsValuesAndNames args) {
        throw RInternalError.shouldNotReachHere("shadowed by the overload with VirtualFrame");
    }
}
