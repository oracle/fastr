/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.data;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInterface;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.DSLConfig;
import com.oracle.truffle.r.runtime.RRuntimeASTAccess;
import com.oracle.truffle.r.runtime.RType;
import com.oracle.truffle.r.runtime.VirtualEvalFrame;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.builtins.RBuiltinDescriptor;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RFunctionFactory.CachedExplicitCallNodeGen;
import com.oracle.truffle.r.runtime.interop.Foreign2R;
import com.oracle.truffle.r.runtime.interop.R2Foreign;
import com.oracle.truffle.r.runtime.interop.R2ForeignNodeGen;
import com.oracle.truffle.r.runtime.data.RSharingAttributeStorage.Shareable;

/**
 * An instance of {@link RFunction} represents a function defined in R. The properties of a function
 * are as follows:
 * <ul>
 * <li>The {@link #name} is optional. It is only set initially for builtins (required).
 * <li>The {@link #target} represents the actually callable entry point to the function.
 * <li>Functions may represent builtins; this is indicated by the {@link #builtin} flag set to the
 * associated {@link RBuiltin} instance.
 * <li>The lexically enclosing environment of this function's definition is referenced by
 * {@link #enclosingFrame}.
 * </ul>
 */
@ExportLibrary(InteropLibrary.class)
public final class RFunction extends RSharingAttributeStorage implements RTypedValue, Shareable {

    public static final String NO_NAME = new String("");

    private final String name;
    private final String packageName;
    @CompilationFinal private RootCallTarget target;
    private final RBuiltinDescriptor builtin;

    @CompilationFinal private MaterializedFrame enclosingFrame;

    RFunction(String name, String packageName, RootCallTarget target, RBuiltinDescriptor builtin, MaterializedFrame enclosingFrame) {
        this.packageName = packageName;
        this.target = target;
        this.builtin = builtin;
        this.name = name;
        if (!isBuiltin() && name != NO_NAME) {
            // If we have a name, propagate it to the rootnode
            RContext.getRRuntimeASTAccess().setFunctionName(getRootNode(), name);
        }
        this.enclosingFrame = enclosingFrame instanceof VirtualEvalFrame ? ((VirtualEvalFrame) enclosingFrame).getOriginalFrame() : enclosingFrame;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    boolean isExecutable() {
        return true;
    }

    @ExportMessage
    Object execute(Object[] arguments,
                    @Cached() ExplicitCall call) {
        return call.execute(this, arguments);
    }

    @Override
    public RType getRType() {
        // Note: GnuR distinguishes "builtins" and "specials" (BUILTINSXP vs SPECIALSXP). The later
        // has non-evaluated args. FastR and GnuR built-ins differ in whether they have evaluated
        // args, so we cannot correctly choose RType.Special here.
        return isBuiltin() ? RType.Builtin : RType.Closure;
    }

    public boolean isBuiltin() {
        return builtin != null;
    }

    public RBuiltinDescriptor getRBuiltin() {
        return builtin;
    }

    public String getName() {
        return name;
    }

    public String getPackageName() {
        return packageName;
    }

    public RootCallTarget getTarget() {
        return target;
    }

    public RootNode getRootNode() {
        return target.getRootNode();
    }

    public MaterializedFrame getEnclosingFrame() {
        return enclosingFrame;
    }

    @Override
    public String toString() {
        return target.toString();
    }

    @Override
    public RFunction copy() {
        RFunction newFunction = RDataFactory.createFunction(getName(), getPackageName(), getTarget(), getRBuiltin(), getEnclosingFrame());
        if (getAttributes() != null) {
            newFunction.initAttributes(RAttributesLayout.copy(getAttributes()));
        }
        newFunction.setTypedValueInfo(getTypedValueInfo());
        return newFunction;
    }

    public void reassignTarget(RootCallTarget newTarget) {
        this.target = newTarget;
    }

    public void reassignEnclosingFrame(MaterializedFrame newEnclosingFrame) {
        this.enclosingFrame = newEnclosingFrame;
    }

    public interface ExplicitCall extends NodeInterface {
        static ExplicitCall create() {
            return DSLConfig.getInteropLibraryCacheSize() > 0 ? CachedExplicitCallNodeGen.create() : new UncachedExplicitCall();
        }

        static ExplicitCall getUncached() {
            return new UncachedExplicitCall();
        }

        Object execute(RFunction function, Object[] args);
    }

    protected abstract static class CachedExplicitCall extends Node implements ExplicitCall {

        @Specialization
        Object call(RFunction function, Object[] arguments,
                        @Cached("createCallNode()") RRuntimeASTAccess.ExplicitFunctionCall callNode,
                        @Cached() Foreign2R foreign2R,
                        @Cached() R2Foreign r2Foreign) {
            Object[] convertedArguments = new Object[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                convertedArguments[i] = foreign2R.convert(arguments[i]);
            }
            MaterializedFrame globalFrame = RContext.getInstance().stateREnvironment.getGlobalFrame();
            RArgsValuesAndNames argsAndValues = new RArgsValuesAndNames(convertedArguments, ArgumentsSignature.empty(arguments.length));
            Object result = callNode.call(globalFrame, function, argsAndValues);
            return r2Foreign.convert(result);
        }

        protected RRuntimeASTAccess.ExplicitFunctionCall createCallNode() {
            return RContext.getRRuntimeASTAccess().createExplicitFunctionCall();
        }
    }

    protected static class UncachedExplicitCall implements ExplicitCall {
        private R2Foreign r2Foreign = R2ForeignNodeGen.getUncached();
        private Foreign2R foreign2R = Foreign2R.getUncached();

        @Override
        public Object execute(RFunction function, Object[] arguments) {
            Object[] convertedArguments = new Object[arguments.length];
            for (int i = 0; i < arguments.length; i++) {
                convertedArguments[i] = foreign2R.convert(arguments[i]);
            }
            return r2Foreign.convert(RContext.getRRuntimeASTAccess().callback(function, convertedArguments));
        }
    }
}
