/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.ffi;

import com.oracle.truffle.api.nodes.NodeInterface;

/**
 * Function down-calls related to the embedded API. TODO: these all should be invoked as proper
 * down-calls because the user code may want to use R API.
 */
public interface REmbedRFFI {
    interface ReadConsoleNode extends NodeInterface {
        String execute(String prompt);

        static REmbedRFFI.ReadConsoleNode create() {
            return RFFIFactory.getREmbedRFFI().createReadConsoleNode();
        }
    }

    interface WriteConsoleBaseNode extends NodeInterface {
        void execute(String x);
    }

    interface WriteConsoleNode extends WriteConsoleBaseNode {
        static REmbedRFFI.WriteConsoleNode create() {
            return RFFIFactory.getREmbedRFFI().createWriteConsoleNode();
        }
    }

    interface WriteErrConsoleNode extends WriteConsoleBaseNode {
        static REmbedRFFI.WriteErrConsoleNode create() {
            return RFFIFactory.getREmbedRFFI().createWriteErrConsoleNode();
        }
    }

    ReadConsoleNode createReadConsoleNode();

    WriteConsoleNode createWriteConsoleNode();

    WriteErrConsoleNode createWriteErrConsoleNode();
}
