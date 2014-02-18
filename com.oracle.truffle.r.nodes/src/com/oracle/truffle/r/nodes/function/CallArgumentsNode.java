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
package com.oracle.truffle.r.nodes.function;

import java.util.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

public final class CallArgumentsNode extends RNode {

    @Children private final RNode[] arguments;

    private final String[] names;
    private final int nameCount;

    private CallArgumentsNode(RNode[] args, String[] names) {
        this.arguments = adoptChildren(args);
        this.names = names;
        this.nameCount = countNonNull(names);
    }

    private CallArgumentsNode(SourceSection src, RNode[] args, String[] names) {
        this(args, names);
        assignSourceSection(src);
    }

    private static int countNonNull(String[] names) {
        int count = 0;
        for (int i = 0; i < names.length; i++) {
            if (names[i] != null) {
                count++;
            }
        }
        return count;
    }

    public RNode[] getArguments() {
        return arguments;
    }

    public String[] getNames() {
        return names;
    }

    public int getNameCount() {
        return nameCount;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return executeArray(frame);
    }

    @Override
    @ExplodeLoop
    public Object[] executeArray(VirtualFrame frame) {
        Object[] values = new Object[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            RNode arg = arguments[i];
            values[i] = arg == null ? RMissing.instance : arg.execute(frame);
        }
        return values;
    }

    public CallArgumentsNode copyOfRange(int startIndex, int endIndex) {
        RNode[] args = Arrays.copyOfRange(arguments, startIndex, endIndex);
        String[] newNames = Arrays.copyOfRange(names, startIndex, endIndex);
        return new CallArgumentsNode(Utils.sourceBoundingBox(args), args, newNames);
    }

    public static CallArgumentsNode createUnnamed(RNode... args) {
        return create(args, null);
    }

    private static final String[] NO_NAMES = new String[0];

    public static CallArgumentsNode create(RNode[] args, String[] names) {
        RNode[] wrappedArgs = new RNode[args.length];
        for (int i = 0; i < wrappedArgs.length; ++i) {
            wrappedArgs[i] = args[i] == null ? null : WrapArgumentNode.create(args[i]);
        }
        String[] resolvedNames = names;
        if (resolvedNames == null) {
            resolvedNames = NO_NAMES;
        }
        return new CallArgumentsNode(Utils.sourceBoundingBox(wrappedArgs), wrappedArgs, resolvedNames);
    }
}
