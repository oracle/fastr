/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import com.oracle.truffle.api.source.Source;

/**
 * A facade for the creation of Truffle {@link Source} objects, which is complicated in R due the
 * the many different ways in which sources can be created. Particularly tricky are sources created
 * from deparsing functions from (binary-form) packages and from the {@code source} builtin, as
 * these are presented as text strings despite being associated with files.
 *
 * We separate sources as <i>internal</i> or <i>external</i>, where the former will return
 * {@code true} to {@link Source#isInternal()}. External sources always correspond to some external
 * data source, e.g., file, url, even if they are not created using the standard methods in
 * {@link Source}. An internal source will always return {@code null} to {@link Source#getURI} but
 * {@link Source#getName} should return a value that indicates how/why the source was created, a
 * value from {@link Internal}.
 *
 */
public class RSource {
    /**
     * Collection of strings that are used to indicate {@link Source} instances that have internal
     * descriptions.
     */
    public enum Internal {

        UNIT_TEST("<unit_test>"),
        SHELL_INPUT("<shell_input>"),
        EXPRESSION_INPUT("<expression_input>"),
        GET_ECHO("<get_echo>"),
        QUIT_EOF("<<quit_eof>>"),
        STARTUP_SHUTDOWN("<startup/shutdown>"),
        REPL_WRAPPER("<repl wrapper>"),
        EVAL_WRAPPER("<eval wrapper>"),
        NO_SOURCE("<no source>"),
        CONTEXT_EVAL("<context_eval>"),
        RF_FINDFUN("<Rf_findfun>"),
        BROWSER_INPUT("<browser_input>"),
        CLEAR_WARNINGS("<clear_warnings>"),
        DEPARSE("<deparse>"),
        GET_CONTEXT("<get_context>"),
        DEBUGTEST_FACTORIAL("<factorial.r>"),
        DEBUGTEST_DEBUG("<debugtest.r>"),
        DEBUGTEST_EVAL("<evaltest.r>"),
        TCK_INIT("<tck_initialization>"),
        PACKAGE("<package:%s deparse>"),
        DEPARSE_ERROR("<package_deparse_error>"),
        LAPPLY("<lapply>"),
        R_PARSEVECTOR("<R_ParseVector>"),
        PAIRLIST_DEPARSE("<pairlist deparse>");

        public final String string;

        Internal(String text) {
            this.string = text;
        }

    }

    /**
     * Create an (external) source from the {@code text} that is known to originate from the file
     * system path {@code path}. The simulates the behavior of {@link #fromFile}.
     */
    public static Source fromFileName(String text, String path) throws URISyntaxException {
        File file = new File(path).getAbsoluteFile();
        URI uri = new URI("file://" + file.getAbsolutePath());
        return Source.newBuilder(text).name(file.getName()).uri(uri).mimeType(RRuntime.R_APP_MIME).build();
    }

    /**
     * Create an {@code internal} source from {@code text} and {@code description}.
     */
    public static Source fromTextInternal(String text, Internal description) {
        return fromTextInternal(text, description, RRuntime.R_APP_MIME);
    }

    /**
     * Create an {@code internal} source from {@code text} and {@code description} of given
     * {@code mimeType}.
     */

    public static Source fromTextInternal(String text, Internal description, String mimeType) {
        return Source.newBuilder(text).name(description.string).mimeType(mimeType).internal().build();
    }

    /**
     * Create an {@code internal} source for a deparsed package from {@code text} and
     * {@code packageName}.
     */
    public static Source fromPackageTextInternal(String text, String packageName) {
        String name = String.format(Internal.PACKAGE.string, packageName);
        return Source.newBuilder(text).name(name).mimeType(RRuntime.R_APP_MIME).build();
    }

    /**
     * Create an {@code internal} source for a deparsed package from {@code text} when the function
     * name might be known. If {@code functionName} is not {@code null}, use it as the "name" for
     * the source, else default to {@link #fromPackageTextInternal(String, String)}.
     */
    public static Source fromPackageTextInternalWithName(String text, String packageName, String functionName) {
        if (functionName == null) {
            return fromPackageTextInternal(text, packageName);
        } else {
            return Source.newBuilder(text).name(packageName + "::" + functionName).mimeType(RRuntime.R_APP_MIME).build();
        }
    }

    /**
     * Create an (external) source from the file system path {@code path}.
     */
    public static Source fromFileName(String path) throws IOException {
        return Source.newBuilder(new File(path)).name(path).mimeType(RRuntime.R_APP_MIME).build();
    }

    /**
     * Create an (external) source from the file system path denoted by {@code file}.
     */
    public static Source fromFile(File file) throws IOException {
        return Source.newBuilder(file).name(file.getName()).mimeType(RRuntime.R_APP_MIME).build();
    }

    /**
     * Create an (external) source from {@code url}.
     */
    public static Source fromURL(URL url, String name) throws IOException {
        return Source.newBuilder(url).name(name).mimeType(RRuntime.R_APP_MIME).build();
    }

    /**
     * If {@code source} was created with {@link #fromPackageTextInternal} return the
     * "package:name", else {@code null}. This can be used to access the corresponding R
     * environment.
     */
    public static String getPackageName(Source source) {
        String sourceName = source.getName();
        if (sourceName.startsWith("<package:")) {
            return sourceName.substring(1, sourceName.lastIndexOf(' '));
        } else {
            return null;
        }
    }

    /**
     * If {@code source} is "internal", return {@code null} else return the file system path
     * corresponding to the associated {@link URI}.
     */
    public static String getPath(Source source) {
        if (source == null || source.isInternal()) {
            return null;
        }
        URI uri = source.getURI();
        assert uri != null;
        return uri.getPath();
    }

    /**
     * Always returns a non-null string even for internal sources.
     */
    public static String getOrigin(Source source) {
        String path = RSource.getPath(source);
        if (path == null) {
            return source.getName();
        } else {
            return path;
        }
    }

}
