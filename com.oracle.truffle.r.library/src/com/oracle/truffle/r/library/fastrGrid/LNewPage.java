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

import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.data.RNull;

class LNewPage extends RExternalBuiltinNode.Arg0 {
    static {
        Casts.noCasts(LNewPage.class);
    }

    @Override
    public Object execute() {
        GridContext.getContext().getCurrentDevice().openNewPage();
        return RNull.instance;
    }
}
