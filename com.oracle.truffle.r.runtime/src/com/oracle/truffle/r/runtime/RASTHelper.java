/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime;

import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;

/**
 * A collection of methods that need access to the AST types, needed by code that resides in the
 * runtime project, which does not have direct access, as it would introduce project circularities.
 *
 */
public interface RASTHelper {
    /**
     * Computes the "length" of the language element as per the R specification.
     */
    int getLength(RLanguage rl);

    /**
     * Returns the object ({@link RSymbol}, {@link RLanguage} or scalar value (e.g. {@link Double})
     * at index {@code index}.
     */
    Object getDataAtAsObject(RLanguage rl, int index);

    /**
     * Converts {@code rl} to a {@link RList}.
     */
    RList asList(RLanguage rl);

    /**
     * Deparse {@code rl}.
     */
    void deparse(RDeparse.State state, RLanguage rl);

    /**
     * Deparse non-builtin function.
     */
    void deparse(RDeparse.State state, RFunction f);

    /**
     * Serialize function {@code f} (not a builtin).
     */
    Object serialize(RSerialize.State state, RFunction f);

    void serializeNode(RSerialize.State state, Object node);

    /**
     * Call out to R to find a namespace during unserialization.
     */
    REnvironment findNamespace(RStringVector name, int depth);

    /**
     * Call out to R to .handleSimpleError.
     */
    void handleSimpleError(RFunction f, RStringVector msg, Object call, int depth);

    /**
     * Call out to R to .signalSimpleWarning.
     */
    void signalSimpleWarning(RStringVector msg, Object call, int depth);
}
