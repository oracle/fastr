/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.library.GenerateLibrary;
import com.oracle.truffle.api.library.Library;
import com.oracle.truffle.api.library.LibraryFactory;

@GenerateLibrary
public abstract class RPairListLibrary extends Library {

    private static final LibraryFactory<RPairListLibrary> FACTORY = LibraryFactory.resolve(RPairListLibrary.class);

    public static LibraryFactory<RPairListLibrary> getFactory() {
        return FACTORY;
    }

    public static RPairListLibrary getUncached() {
        return FACTORY.getUncached();
    }

    public abstract void setCar(Object target, Object value);

    public abstract void setTag(Object target, Object value);

    public abstract Object car(Object target);

    public abstract Object cdr(Object target);

    public abstract void appendToEnd(Object target, RPairList newEnd);

    public abstract Object getTag(Object target);

    public abstract Closure getClosure(Object target);

    public abstract int getLength(Object target);
}
