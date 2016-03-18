/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.atomic.AtomicLong;

import com.oracle.truffle.r.runtime.FunctionUID;
import com.oracle.truffle.r.runtime.instrument.FunctionUIDFactory;

public class ALONGFunctionUIDFactory extends FunctionUIDFactory {

    private static final AtomicLong ID = new AtomicLong();

    private static final class ALongFunctionUID implements FunctionUID {

        private final long uuid;

        private ALongFunctionUID(long uuid) {
            this.uuid = uuid;
        }

        @Override
        public int compareTo(FunctionUID o) {
            ALongFunctionUID oa = (ALongFunctionUID) o;
            if (uuid == oa.uuid) {
                return 0;
            } else if (uuid < oa.uuid) {
                return -1;
            } else {
                return 1;
            }
        }

        @Override
        public String toString() {
            return Long.toString(uuid);
        }
    }

    @Override
    public FunctionUID createUID() {
        return new ALongFunctionUID(ID.incrementAndGet());
    }
}
