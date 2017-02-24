/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin;

import com.oracle.truffle.api.dsl.GeneratedBy;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.ErrorContext;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.builtins.RBuiltinDescriptor;
import com.oracle.truffle.r.runtime.builtins.RBuiltinKind;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RTypes;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxCall;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;
import com.oracle.truffle.r.runtime.nodes.builtin.RBuiltinBaseNode;

@TypeSystemReference(RTypes.class)
public abstract class RBuiltinNode extends RBuiltinBaseNode implements NodeWithArgumentCasts {

    public abstract Object executeBuiltin(VirtualFrame frame, Object... args);

    /**
     * Return the default values of the builtin's formal arguments. This is only valid for builtins
     * of {@link RBuiltinKind kind} PRIMITIVE or SUBSTITUTE. Only simple scalar constants and
     * {@link RMissing#instance}, {@link RNull#instance} and {@link RArgsValuesAndNames#EMPTY} are
     * allowed.
     */
    public Object[] getDefaultParameterValues() {
        return EMPTY_OBJECT_ARRAY;
    }

    public static final RBuiltinNode inline(RBuiltinDescriptor factory) {
        // static number of arguments
        return ((RBuiltinFactory) factory).getConstructor().get();
    }

    protected final RBuiltin getRBuiltin() {
        return getRBuiltin(getClass());
    }

    private static RBuiltin getRBuiltin(Class<?> klass) {
        GeneratedBy generatedBy = klass.getAnnotation(GeneratedBy.class);
        if (generatedBy != null) {
            return generatedBy.value().getAnnotation(RBuiltin.class);
        } else {
            return null;
        }
    }

    /**
     * Generally, {@link RBuiltinNode} instances are created as child nodes of a private class in
     * {@link RCallNode} that can return the original {@link RCallNode} which has all the pertinent
     * information as initially parsed. However, currently, builtins called via
     * {@code do.call("func", )} have a {@link RBuiltinRootNode} as a parent, which carries no
     * context about the original call, so we return {@code null}.
     */
    public RSyntaxElement getOriginalCall() {
        Node p = getParent();
        while (p != null) {
            if (p instanceof RSyntaxCall) {
                RSyntaxCall call = (RSyntaxCall) p;
                if (call.getSyntaxArguments().length > 0 && call.getSyntaxLHS() instanceof RSyntaxLookup && ((RSyntaxLookup) call.getSyntaxLHS()).getIdentifier().equals(".Internal")) {
                    // unwrap .Internal calls
                    return call.getSyntaxArguments()[0];
                }
                return call;
            }
            p = p.getParent();
        }
        return null;
    }

    @Override
    public String toString() {
        return (getRBuiltin() == null ? getClass().getSimpleName() : getRBuiltin().name());
    }

    @Override
    protected RBaseNode getErrorContext() {
        ErrorContext context = RError.contextForBuiltin(getRBuiltin());
        return context == null ? this : context;
    }
}
