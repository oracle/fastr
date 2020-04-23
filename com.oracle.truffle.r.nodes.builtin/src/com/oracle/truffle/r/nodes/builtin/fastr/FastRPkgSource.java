/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.nodes.builtin.fastr;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.toBoolean;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.io.IOException;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
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
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.context.TruffleRLanguage;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPromise;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import java.io.Writer;

/**
 * Outputs the deparsed source for functions in one or more loaded packages to the
 * {@code Rpkgsource} directory., for use by e.g., a source-level debugger.
 */
@RBuiltin(name = ".fastr.pkgsource", kind = PRIMITIVE, visibility = OFF, parameterNames = {"pkgs", "verbose"}, behavior = COMPLEX)
public abstract class FastRPkgSource extends RBuiltinNode.Arg2 {
    public static final String PKGSOURCE_PROJECT = "Rpkgsource";
    private static final String SLASH_SWAP = "_slash_";

    @Override
    public Object[] getDefaultParameterValues() {
        return new Object[]{RNull.instance, RRuntime.LOGICAL_FALSE};
    }

    static {
        Casts casts = new Casts(FastRPkgSource.class);
        casts.arg("pkgs").allowNull().mustBe(stringValue());
        casts.arg("verbose").asLogicalVector().findFirst().mustNotBeNA().map(toBoolean());
    }

    @Specialization
    public RNull pkgSource(VirtualFrame frame, @SuppressWarnings("unused") RNull pkgs, boolean verbose,
                    @CachedContext(TruffleRLanguage.class) TruffleLanguage.ContextReference<RContext> ctxRef) {
        CompilerDirectives.transferToInterpreter();
        String[] searchPath = REnvironment.searchPath();
        for (String s : searchPath) {
            REnvironment env = REnvironment.lookupOnSearchPath(s);
            String pkg = env.isPackageEnv();
            if (pkg != null) {
                pkgSource(frame, RDataFactory.createStringVectorFromScalar(pkg.replace("package:", "")), verbose, ctxRef);
            }
        }
        return RNull.instance;
    }

    @Specialization
    public RNull pkgSource(VirtualFrame frame, RStringVector pkgs, boolean verbose,
                    @CachedContext(TruffleRLanguage.class) TruffleLanguage.ContextReference<RContext> ctxRef) {
        CompilerDirectives.transferToInterpreter();
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
                                saveSource(ctxRef.get(), pkg, name, deparseResult);
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
            throw error(RError.Message.GENERIC, ex.getMessage());
        }
    }

    @TruffleBoundary
    private void notFound(String pkg) {
        warning(RError.Message.GENERIC, String.format("namespace '%s' not found - ignoring", pkg));
    }

    @TruffleBoundary
    private void noDeparse(String pkg, String fname) {
        warning(RError.Message.GENERIC, String.format("function '%s::%s' failed to deparse - ignoring", pkg, fname));
    }

    @TruffleBoundary
    private void saveSource(RContext context, String pkg, String fname, String deparseResult) {
        RSerialize.setSaveDeparse(false);
        try {
            TruffleFile target = targetPath(context, pkg, fname);
            try (Writer wr = target.newBufferedWriter()) {
                wr.write(deparseResult);
            }
        } catch (IOException ex) {
            throw error(RError.Message.GENERIC, ex.getMessage());
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

    private static TruffleFile targetPath(RContext context, String pkg, String fnameArg) throws IOException {
        TruffleFile targetDir = REnvVars.getRHomeTruffleFile(context).resolve(PKGSOURCE_PROJECT).resolve(pkg);
        targetDir.createDirectories();
        String fname = mungeName(fnameArg);
        TruffleFile target = targetDir.resolve(fname + ".R");
        return target;
    }

}
