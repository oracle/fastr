/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.*;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * R.home builtin.
 *
 * TODO This is currently a complete implementation of the {@code R.home} functionality, as
 * {@code switch} is not implemented, which is used by the R code for {@code R.home} in
 * {@code files.R}.
 *
 * N.B. GnuR seems to allow other types as the component, e.g. integer, although the spec does not
 * mention coercions.
 */
@RBuiltin(name = "R.home", kind = INTERNAL)
// TODO revert to R implementation
public abstract class Rhome extends RBuiltinNode {

    @Specialization(order = 0)
    public Object doRhome(@SuppressWarnings("unused") RMissing component) {
        controlVisibility();
        return RDataFactory.createStringVector(REnvVars.rHome());
    }

    @Specialization(order = 1)
    public Object doRhome(String component) {
        controlVisibility();
        String rHome = REnvVars.rHome();
        String result = component.equals("home") ? rHome : RRuntime.toString(FileSystems.getDefault().getPath(rHome, component).toAbsolutePath());
        return RDataFactory.createStringVector(result);
    }

}
