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
package com.oracle.truffle.r.library.utils;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.notEmpty;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef;
import com.oracle.truffle.r.nodes.builtin.RExternalBuiltinNode;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;

/**
 * Support for the "internal"method of "utils::download.file". TODO take note of "quiet", "mode" and
 * "cacheOK".
 */
public abstract class Download extends RExternalBuiltinNode.Arg5 {

    @Override
    protected void createCasts(CastBuilder casts) {
        casts.arg(0).mustNotBeNull().mustBe(stringValue()).asStringVector().mustBe(notEmpty()).shouldBe(singleElement(), Message.ONLY_FIRST_USED).findFirst();
        casts.arg(1).mustNotBeNull().mustBe(stringValue()).asStringVector().mustBe(notEmpty()).shouldBe(singleElement(), Message.ONLY_FIRST_USED).findFirst();
        casts.arg(2).mustNotBeNull().mustBe(Predef.logicalValue()).asLogicalVector().mustBe(notEmpty()).shouldBe(singleElement(), Message.ONLY_FIRST_USED).findFirst().map(toBoolean());
        casts.arg(3).mustNotBeNull().mustBe(stringValue()).asStringVector().mustBe(notEmpty()).shouldBe(singleElement(), Message.ONLY_FIRST_USED).findFirst();
        casts.arg(4).mustNotBeNull().mustBe(Predef.logicalValue()).asLogicalVector().mustBe(notEmpty()).shouldBe(singleElement(), Message.ONLY_FIRST_USED).findFirst().map(toBoolean());

    }

    @Specialization
    protected int download(String urlString, String destFile, @SuppressWarnings("unused") boolean quiet, @SuppressWarnings("unused") String mode, @SuppressWarnings("unused") boolean cacheOK) {
        try {
            try (InputStream in = new URL(urlString).openStream()) {
                Files.copy(in, Paths.get(destFile), StandardCopyOption.REPLACE_EXISTING);
            }
            return 0;
        } catch (IOException e) {
            errorProfile.enter();
            throw RError.error(this, RError.Message.GENERIC, e.getMessage());
        }
    }
}
