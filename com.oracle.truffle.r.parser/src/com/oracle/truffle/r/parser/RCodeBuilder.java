/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.parser;

import java.util.List;

import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.RError;

public interface RCodeBuilder<T, ArgT> {

    ArgT argument(SourceSection source, String name, T expression);

    ArgT argument(T expression);

    ArgT argumentEmpty();

    T call(SourceSection source, T lhs, @SuppressWarnings("unchecked") ArgT... arguments);

    T call(SourceSection source, T lhs, List<ArgT> arguments);

    T constant(SourceSection source, Object value);

    T lookup(SourceSection source, String symbol, boolean functionLookup);

    T function(SourceSection source, List<ArgT> arguments, T body);

    void warning(RError.Message message, Object... arguments);
}
