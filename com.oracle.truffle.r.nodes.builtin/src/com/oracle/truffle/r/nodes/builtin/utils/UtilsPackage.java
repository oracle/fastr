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

package com.oracle.truffle.r.nodes.builtin.utils;

import com.oracle.truffle.r.nodes.builtin.*;

public class UtilsPackage extends RBuiltinPackage {
    public UtilsPackage() {
        loadBuiltins();
    }

    @Override
    public String getName() {
        return "utils";
    }
}
