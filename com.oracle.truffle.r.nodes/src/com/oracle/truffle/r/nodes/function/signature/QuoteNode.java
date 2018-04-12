/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.function.signature;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.RASTUtils;
import com.oracle.truffle.r.nodes.control.OperatorNode;
import com.oracle.truffle.r.nodes.function.visibility.SetVisibilityNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.nodes.RSyntaxElement;
import com.oracle.truffle.r.runtime.nodes.RSyntaxLookup;

public final class QuoteNode extends OperatorNode {

    @Child private SetVisibilityNode visibility = SetVisibilityNode.create();

    private final ArgumentsSignature signature;
    private final RSyntaxElement[] args;

    @CompilationFinal private Object value;

    public QuoteNode(SourceSection source, RSyntaxLookup operator, ArgumentsSignature signature, RSyntaxElement[] args) {
        super(source, operator);
        this.signature = signature;
        this.args = args;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (value == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            if (args.length != 1) {
                throw error(Message.ARGUMENTS_PASSED, args.length, "'missing'", 1);
            }
            String name = signature.getName(0);
            if (name != null && !name.isEmpty() && !"expr".equals(name)) {
                throw error(Message.ARGUMENT_NOT_MATCH, name, "expr");
            }
            value = args[0] == null ? RSymbol.MISSING : RASTUtils.createLanguageElement(args[0]);
        }
        visibility.execute(frame, true);
        return value;
    }

    @Override
    public ArgumentsSignature getSyntaxSignature() {
        return signature;
    }

    @Override
    public RSyntaxElement[] getSyntaxArguments() {
        return args;
    }
}
