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
package com.oracle.truffle.r.runtime;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import com.oracle.truffle.r.runtime.context.RContext;

public class LazyDBCache {

    public static final class ContextStateImpl implements RContext.ContextState {
        private Map<String, byte[]> dbCache = new HashMap<>();

        public byte[] getData(String dbPath) {
            byte[] dbData = dbCache.get(dbPath);
            if (dbData == null) {
                try {
                    dbData = Files.readAllBytes(FileSystems.getDefault().getPath(dbPath));
                } catch (IOException ex) {
                    // unexpected
                    throw RInternalError.shouldNotReachHere(ex);
                }
                dbCache.put(dbPath, dbData);
            }
            return dbData;
        }

        public void remove(String dbPath) {
            // no an error if missing
            dbCache.remove(dbPath);
        }

        public static ContextStateImpl newContextState() {
            return new ContextStateImpl();
        }
    }
}
