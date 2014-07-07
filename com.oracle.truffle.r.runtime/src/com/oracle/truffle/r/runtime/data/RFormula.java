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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.source.*;

/**
 * Represents a formula, i.e., an expression of the form {@code response ~ model}.
 *
 * Currently, only very simple formulae of the form {@code y ~ x} are supported. Both response and
 * model are represented internally as {@link Object} members.
 */
public class RFormula extends RScalar {

    private final SourceSection source;
    private final Object response;
    private final Object model;

    public RFormula(SourceSection source, Object response, Object model) {
        this.source = source;
        this.response = response;
        this.model = model;
    }

    public SourceSection getSource() {
        return source;
    }

    public Object getResponse() {
        return response;
    }

    public Object getModel() {
        return model;
    }

}
