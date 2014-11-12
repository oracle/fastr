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

import com.oracle.truffle.api.CompilerDirectives.*;
import com.oracle.truffle.r.nodes.*;

/**
 * Simple generic base class for pairs of {@link #arguments} and {@link #names} (that are not
 * {@link RNode}s).
 *
 * @param <T> The type of {@link #arguments}
 */
public abstract class Arguments<T> implements ArgumentsTrait {

    /**
     * Array of arguments; semantics have to be specified by child classes.
     */
    @CompilationFinal protected final T[] arguments;

    /**
     * Array of arguments; semantics have to be specified by child classes.
     */
    @CompilationFinal protected final String[] names;

    /**
     * Cache use for {@link #getNameCount()}.
     */
    private Integer nameCountCache = null;

    Arguments(T[] arguments, String[] names) {
        this.arguments = arguments;
        this.names = names;
    }

    /**
     * @return See {@link ArgumentsTrait#getNameCount()}
     */
    public int getNameCount() {
        if (nameCountCache == null) {
            nameCountCache = ArgumentsTrait.super.getNameCount();
        }
        return nameCountCache;
    }
}
