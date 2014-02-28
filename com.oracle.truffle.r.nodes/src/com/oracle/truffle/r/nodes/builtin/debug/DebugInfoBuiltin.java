/*
 * Copyright (c) 2013, 2014, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.debug;

import java.util.*;

import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;

@RBuiltin({"debug.info"})
@RBuiltinComment("Prints this message.")
public abstract class DebugInfoBuiltin extends RBuiltinNode {

    @SlowPath
    @Specialization
    public Object printTree() {
        RBuiltinPackages packages = (RBuiltinPackages) RContext.getInstance().getLookup();
        StringBuilder b = new StringBuilder();
        for (RBuiltinPackage pack : packages.getPackages()) {
            b.append(createPackageString(pack));
        }
        return b.toString();
    }

    @SlowPath
    private static String createPackageString(RBuiltinPackage pack) {
        Map<String, RBuiltinFactory> builtins = pack.getBuiltins();
        StringBuilder msg = new StringBuilder();
        msg.append(String.format("%s functions: %n", pack.getName()));
        for (String name : builtins.keySet()) {
            RBuiltinFactory factory = builtins.get(name);
            RBuiltinComment commentAnnotation = factory.getFactory().getNodeClass().getAnnotation(RBuiltinComment.class);
            String comment = null;
            if (commentAnnotation != null) {
                comment = commentAnnotation.value();
            }

            if (comment == null || comment.isEmpty()) {
                comment = "";
            }

            msg.append(String.format(" - %s : %s%n", name, comment));
        }
        return msg.toString();
    }
}
