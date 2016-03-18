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
package com.oracle.truffle.r.runtime.ffi.jnr;

import java.util.ArrayList;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI;
import com.oracle.truffle.r.runtime.ffi.BaseRFFI.UtsName;
import com.oracle.truffle.r.runtime.ffi.LibPaths;

/**
 * Additional support for {@link BaseRFFI} that is implemented using JNI not JNR.
 */
public class JNI_OSExtras {
    private interface OSExtras {
        UtsName uname();

        ArrayList<String> glob(String pattern);
    }

    private static class OSExtraProvider implements OSExtras {
        private static OSExtras osExtras;

        @TruffleBoundary
        private static OSExtras createAndLoadLib() {
            try {
                System.load(LibPaths.getBuiltinLibPath("osextras"));
                return new OSExtraProvider();
            } catch (UnsatisfiedLinkError ex) {
                throw RInternalError.shouldNotReachHere("osextras");
            }
        }

        static OSExtras osExtras() {
            if (osExtras == null) {
                osExtras = createAndLoadLib();
            }
            return osExtras;
        }

        @Override
        public UtsName uname() {
            return JNI_UtsName.get();
        }

        @Override
        public ArrayList<String> glob(String pattern) {
            return JNI_Glob.glob(pattern);
        }
    }

    private static OSExtras osExtras() {
        return OSExtraProvider.osExtras();
    }

    static UtsName uname() {
        return osExtras().uname();
    }

    static ArrayList<String> glob(String pattern) {
        return osExtras().glob(pattern);
    }
}
