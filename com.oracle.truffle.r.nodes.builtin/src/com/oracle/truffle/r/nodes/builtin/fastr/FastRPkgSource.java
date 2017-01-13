/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.fastr;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.function.FunctionDefinitionNode;
import com.oracle.truffle.r.nodes.function.PromiseHelperNode;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.REnvVars;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.RSerialize;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.conn.StdConnections;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;

/**
 * Outputs the deparsed source for functions in one or more loaded packages to the
 * {@code Rpkgsource} directory., for use by e.g., a source-level debugger.
 */
@RBuiltin(name = ".fastr.pkgsource", kind = PRIMITIVE, visibility = OFF, parameterNames = {"pkgs", "verbose"}, behavior = COMPLEX)
public abstract class FastRPkgSource extends RBuiltinNode {
    public static final String PKGSOURCE_PROJECT = "Rpkgsource";
    private static final String SLASH_SWAP = "_slash_";

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RNull.instance, RRuntime.LOGICAL_FALSE};
    }

    @Override
    public void createCasts(CastBuilder casts) {
        casts.arg("pkgs").mustBe(stringValue());
        casts.arg("verbose").asLogicalVector().findFirst().notNA().map(toBoolean());
    }

    @Specialization
    public RNull pkgSource(VirtualFrame frame, @SuppressWarnings("unused") RNull pkgs, boolean verbose) {
        String[] searchPath = REnvironment.searchPath();
        for (String s : searchPath) {
            REnvironment env = REnvironment.lookupOnSearchPath(s);
            String pkg = env.isPackageEnv();
            if (pkg != null) {
                pkgSource(frame, RDataFactory.createStringVectorFromScalar(pkg.replace("package:", "")), verbose);
            }
        }
        return RNull.instance;
    }

    @Specialization
    public RNull pkgSource(VirtualFrame frame, RAbstractStringVector pkgs, boolean verbose) {
        for (int i = 0; i < pkgs.getLength(); i++) {
            String pkg = pkgs.getDataAt(i);
            REnvironment env = REnvironment.getRegisteredNamespace(pkg);
            if (env == null) {
                notFound(pkg);
            } else {
                if (verbose) {
                    output("processing package: ", false);
                    output(pkg, true);
                }
                RStringVector names = env.ls(true, null, false);
                for (int n = 0; n < names.getLength(); n++) {
                    String name = names.getDataAt(n);
                    Object value = env.get(name);
                    try {
                        if (value instanceof RPromise) {
                            value = PromiseHelperNode.evaluateSlowPath(frame, (RPromise) value);
                        }
                        if (value instanceof RFunction) {
                            RFunction fun = (RFunction) value;
                            if (!fun.isBuiltin()) {
                                if (verbose) {
                                    output("processing function ", false);
                                    output(name, true);
                                }
                                String deparseResult = RDeparse.deparseSyntaxElement((FunctionDefinitionNode) fun.getRootNode());
                                saveSource(pkg, name, deparseResult);
                            }
                        }
                    } catch (Throwable t) {
                        noDeparse(pkg, name);
                    }
                }
            }
        }
        return RNull.instance;
    }

    @TruffleBoundary
    private void output(String msg, boolean nl) {
        try {
            StdConnections.getStdout().writeString(msg, nl);
        } catch (IOException ex) {
            throw RError.error(this, RError.Message.GENERIC, ex.getMessage());
        }
    }

    @TruffleBoundary
    private void notFound(String pkg) {
        RError.warning(this, RError.Message.GENERIC, String.format("namespace '%s' not found - ignoring", pkg));
    }

    @TruffleBoundary
    private void noDeparse(String pkg, String fname) {
        RError.warning(this, RError.Message.GENERIC, String.format("function '%s::%s' failed to deparse - ignoring", pkg, fname));
    }

    @TruffleBoundary
    private static void saveSource(String pkg, String fname, String deparseResult) {
        RSerialize.setSaveDeparse(false);
        try {
            Path target = targetPath(pkg, fname);
            try (FileWriter wr = new FileWriter(target.toFile())) {
                wr.write(deparseResult);
            }
        } catch (IOException ex) {
            throw RError.error(RError.SHOW_CALLER2, RError.Message.GENERIC, ex.getMessage());
        }
    }

    /**
     * Some function names do not map into useful or even legal filenames. This method takes care of
     * that.
     */
    private static String mungeName(String fname) {
        String result = fname;
        result = result.replace("/", SLASH_SWAP);
        return result;
    }

    private static Path targetPath(String pkg, String fnameArg) throws IOException {
        Path targetDir = dirPath().resolve(pkg);
        Files.createDirectories(targetDir);
        String fname = mungeName(fnameArg);
        Path target = targetDir.resolve(fname + ".R");
        return target;
    }

    private static Path dirPath() {
        return FileSystems.getDefault().getPath(REnvVars.rHome(), PKGSOURCE_PROJECT);
    }

}
