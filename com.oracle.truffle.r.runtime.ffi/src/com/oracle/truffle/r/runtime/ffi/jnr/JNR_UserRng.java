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

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.ffi.UserRngRFFI;

import jnr.ffi.LibraryLoader;
import jnr.ffi.Pointer;
import jnr.ffi.annotations.In;

//Checkstyle: stop method name
public class JNR_UserRng implements UserRngRFFI {
    public interface UserRng {
        void user_unif_init(@In int seed);

        Pointer user_unif_rand();

        Pointer user_unif_nseed();

        Pointer user_unif_seedloc();
    }

    private static class UserRngProvider {
        private static String libPath;
        private static UserRng userRng;

        UserRngProvider(String libPath) {
            UserRngProvider.libPath = libPath;
        }

        @TruffleBoundary
        private static UserRng createAndLoadLib() {
            return LibraryLoader.create(UserRng.class).load(libPath);
        }

        static UserRng userRng() {
            if (userRng == null) {
                userRng = createAndLoadLib();
            }
            return userRng;
        }
    }

    private static UserRng userRng() {
        return UserRngProvider.userRng();
    }

    @Override
    @SuppressWarnings("unused")
    public void setLibrary(String path) {
        new UserRngProvider(path);

    }

    @Override
    @TruffleBoundary
    public void init(int seed) {
        userRng().user_unif_init(seed);
    }

    @Override
    @TruffleBoundary
    public double rand() {
        Pointer pDouble = userRng().user_unif_rand();
        return pDouble.getDouble(0);
    }

    @Override
    @TruffleBoundary
    public int nSeed() {
        return userRng().user_unif_nseed().getInt(0);
    }

    @Override
    @TruffleBoundary
    public void seeds(int[] n) {
        Pointer pInt = userRng().user_unif_seedloc();
        for (int i = 0; i < n.length; i++) {
            n[i] = pInt.getInt(i * 4);
        }
    }
}
