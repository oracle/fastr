/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.engine;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.context.Engine.ParseException;
import com.oracle.truffle.r.runtime.nodes.RInstrumentableNode;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * Node that represents piece of AST that would throw syntax error when parser. Used to delay
 * parsing error to the point when they should be reported.
 */
final class SyntaxErrorNode extends RNode implements RSyntaxNode, RInstrumentableNode {
    private final ParseException exception;
    private final SourceSection sourceSection;

    SyntaxErrorNode(ParseException exception, SourceSection sourceSection) {
        this.exception = exception;
        this.sourceSection = sourceSection;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        throw exception.throwAsRError();
    }

    @Override
    public SourceSection getSourceSection() {
        return this.sourceSection;
    }

    @Override
    public SourceSection getLazySourceSection() {
        return this.sourceSection;
    }

    @Override
    public void setSourceSection(SourceSection source) {
        throw RInternalError.shouldNotReachHere(String.format("%s should not be serialized/deserialzed.", getClass().getSimpleName()));
    }
}
