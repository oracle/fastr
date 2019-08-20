/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.env;

import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Scope;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;
import com.oracle.truffle.r.runtime.env.RScope.VariablesObject;
import com.oracle.truffle.r.runtime.env.frame.REnvEmptyFrameAccess;
import com.oracle.truffle.r.runtime.env.frame.REnvFrameAccess;
import com.oracle.truffle.r.runtime.env.frame.REnvTruffleFrameAccess;
import com.oracle.truffle.r.runtime.interop.Foreign2R;
import com.oracle.truffle.r.runtime.interop.R2Foreign;

/**
 * Represents a variable scope for external tools like a debugger.<br>
 * This is basically a view on R environments.
 */
public final class RScope {

    private final Node current;
    private REnvironment env;

    /**
     * Intended to be used when creating a parent scope where we do not know any associated node.
     */
    private RScope(REnvironment env) {
        // the environment may not have been initialized yet when debugging the LLVM version of libR
        this.env = env == null ? REnvironment.emptyEnv() : env;
        this.current = null;
    }

    private RScope(Node current, REnvironment env) {
        this.current = current;
        this.env = env == null ? REnvironment.emptyEnv() : env;
    }

    protected String getName() {
        // just to be sure
        if (env == REnvironment.emptyEnv()) {
            return "empty environment";
        }

        assert env.getFrame() != null;
        RFunction function = RArguments.getFunction(env.getFrame());
        if (function != null) {
            String name = function.getName();
            return "function environment" + (name != null ? " for function " + name : "");
        } else {
            String name = env.getName();
            return "explicit environment" + (name != null ? ": " + name : "");
        }
    }

    protected Node getNode() {
        return current;
    }

    protected Object getVariables() {
        return new EnvVariablesObject(env, false);
    }

    private static REnvironment getEnv(Frame frame) {
        if (RArguments.isRFrame(frame)) {
            return REnvironment.frameToEnvironment(frame.materialize());
        }
        return null;
    }

    protected Object getArguments() {
        return new EnvVariablesObject(env, true);
    }

    protected RScope findParent() {
        if (env == REnvironment.emptyEnv() || env.getParent() == REnvironment.emptyEnv()) {
            return null;
        }
        return new RScope(env.getParent());
    }

    public static Iterable<Scope> createLocalScopes(RContext context, Node node, Frame frame) {
        if (frame == null) {
            // All variables are created dynamically in R, we could provide at least formal argument
            // names, but note that during the runtime the formal argument may not be provided.
            return Collections.emptySet();
        }
        REnvironment env = getEnv(frame);
        if (env == context.stateREnvironment.getGlobalEnv()) {
            return Collections.emptySet();
        }
        if (env != null && env != REnvironment.emptyEnv()) {
            RScope scope = new RScope(node.getRootNode(), env);
            return createScopes(scope, context.stateREnvironment.getGlobalEnv());
        }
        MaterializedFrame mFrame = frame.materialize();
        String name = node.getRootNode().getName();
        if (name == null) {
            name = "local";
        }
        return Collections.singleton(Scope.newBuilder(name, new GenericVariablesObject(mFrame, false)).node(node).arguments(new GenericVariablesObject(mFrame, true)).build());
    }

    public static Iterable<Scope> createTopScopes(RContext context) {
        REnvironment env = context.stateREnvironment.getGlobalEnv();
        RScope scope = new RScope(env);
        return createScopes(scope, null);
    }

    private static Iterable<Scope> createScopes(RScope scope, REnvironment toEnv) {
        return new Iterable<Scope>() {
            @Override
            public Iterator<Scope> iterator() {
                return new Iterator<Scope>() {
                    private RScope previousScope;
                    private RScope nextScope = scope;

                    @Override
                    public boolean hasNext() {
                        if (nextScope == null) {
                            nextScope = previousScope.findParent();
                            if (nextScope != null && nextScope.env == toEnv) {
                                nextScope = null;
                            }
                        }
                        return nextScope != null;
                    }

                    @Override
                    public Scope next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        Scope vscope = Scope.newBuilder(nextScope.getName(), nextScope.getVariables()).node(nextScope.getNode()).arguments(nextScope.getArguments()).build();
                        previousScope = nextScope;
                        nextScope = null;
                        return vscope;
                    }
                };
            }
        };
    }

    /**
     * Explicitly convert some known types to interop types.
     */
    private static Object getInteropValue(Object obj) {
        if (obj instanceof Frame) {
            MaterializedFrame materialized = ((Frame) obj).materialize();
            assert RArguments.isRFrame(materialized);
            return REnvironment.frameToEnvironment(materialized);
        }
        return obj;
    }

    @ExportLibrary(InteropLibrary.class)
    abstract static class VariablesObject implements TruffleObject {

        private final REnvFrameAccess frameAccess;
        private final boolean argumentsOnly;

        private VariablesObject(REnvFrameAccess frameAccess, boolean argumentsOnly) {
            this.frameAccess = frameAccess;
            this.argumentsOnly = argumentsOnly;
        }

        protected abstract String[] collectArgs();

        protected abstract Object getArgument(String name);

        private String[] ls() {
            RStringVector ls = frameAccess.ls(true, null, false);
            // we make a defensive copy, another option would be to make the vector shared and reuse
            // the underlying array
            return ls.getDataCopy();
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        boolean hasMembers() {
            return true;
        }

        @ExportMessage
        boolean isMemberInsertable(String identifier) {
            return !exists(identifier);
        }

        @ExportMessage
        boolean isMemberModifiable(String identifier) {
            if (!exists(identifier)) {
                return false;
            }
            return frameAccess != null && !frameAccess.bindingIsLocked(identifier);
        }

        @ExportMessage
        boolean isMemberInvocable(String identifier) {
            if (!exists(identifier)) {
                return false;
            }
            return frameAccess != null && get(identifier) instanceof RFunction;
        }

        @ExportMessage
        @TruffleBoundary
        public boolean hasMemberReadSideEffects(String identifier) {
            if (!exists(identifier)) {
                return false;
            }
            return frameAccess != null && isActiveBinding(identifier);
        }

        @ExportMessage
        @TruffleBoundary
        public boolean hasMemberWriteSideEffects(String identifier) {
            if (!exists(identifier)) {
                return false;
            }
            return frameAccess != null && isActiveBinding(identifier);
        }

        @ExportMessage
        @TruffleBoundary
        public Object getMembers(@SuppressWarnings("unused") boolean internal) {
            String[] names = null;
            if (argumentsOnly) {
                names = collectArgs();
            } else {
                names = ls();
            }
            return new ArgumentNamesObject(names);
        }

        private boolean exists(String identifier) {
            for (String key : ls()) {
                if (identifier.equals(key)) {
                    return true;
                }
            }
            return false;
        }

        @ExportMessage
        boolean isMemberReadable(String identifier) {
            return frameAccess != null && exists(identifier);
        }

        @ExportMessage
        Object readMember(String identifier,
                        @Cached() R2Foreign r2Foreign,
                        @Shared("unknownIdentifier") @Cached("createBinaryProfile()") ConditionProfile unknownIdentifier) throws UnsupportedMessageException, UnknownIdentifierException {
            if (frameAccess == null) {
                throw UnsupportedMessageException.create();
            }
            Object value = getValue(identifier);

            // If Java-null is returned, the identifier does not exist !
            if (unknownIdentifier.profile(value == null)) {
                throw UnknownIdentifierException.create(identifier);
            } else {
                return r2Foreign.convert(getInteropValue(value));
            }
        }

        private Object getValue(String identifier) {
            Object value = get(identifier);
            if (value == null) {
                // internal builtin argument?
                value = getArgument(identifier);
            }
            return value;
        }

        @ExportMessage
        void writeMember(String identifier, Object value,
                        @Cached() Foreign2R foreign2R) throws UnsupportedMessageException {
            if (frameAccess == null) {
                throw UnsupportedMessageException.create();
            }
            try {
                put(identifier, foreign2R.convert(value));
            } catch (PutException e) {
                throw RInternalError.shouldNotReachHere(e);
            }
        }

        @ExportMessage
        Object invokeMember(String identifier, Object[] args,
                        @Cached() RFunction.ExplicitCall c,
                        @Shared("unknownIdentifier") @Cached("createBinaryProfile()") ConditionProfile unknownIdentifier,
                        @Exclusive @Cached("createBinaryProfile()") ConditionProfile isFunction) throws UnsupportedMessageException, UnknownIdentifierException {
            if (frameAccess == null) {
                throw UnsupportedMessageException.create();
            }
            Object value = getValue(identifier);
            // If Java-null is returned, the identifier does not exist !
            if (unknownIdentifier.profile(value == null)) {
                throw UnknownIdentifierException.create(identifier);
            } else if (isFunction.profile(value instanceof RFunction)) {
                return c.execute((RFunction) value, args);
            } else {
                throw UnsupportedMessageException.create();
            }
        }

        @TruffleBoundary
        private Object get(String identifier) {
            return frameAccess.get(identifier);
        }

        @TruffleBoundary
        private void put(String identifier, Object value) throws PutException {
            frameAccess.put(identifier, value);
        }

        @TruffleBoundary
        private boolean isActiveBinding(String identifier) {
            return frameAccess.isActiveBinding(identifier);
        }
    }

    static final class EnvVariablesObject extends VariablesObject {

        private final REnvironment env;

        private EnvVariablesObject(REnvironment env, boolean argumentsOnly) {
            super(env.getFrame() == null ? new REnvEmptyFrameAccess() : new REnvTruffleFrameAccess(env.getFrame()), argumentsOnly);
            this.env = env;
        }

        @Override
        protected String[] collectArgs() {
            if (env != REnvironment.emptyEnv()) {
                assert RArguments.isRFrame(env.getFrame());
                RFunction f = RArguments.getFunction(env.getFrame());
                if (f != null) {
                    ArgumentsSignature signature = RContext.getRRuntimeASTAccess().getArgumentsSignature(f);
                    String[] names = signature.getNames();
                    return names == null ? new String[signature.getLength()] : names;
                } else {
                    ArgumentsSignature suppliedSignature = RArguments.getSuppliedSignature(env.getFrame());
                    if (suppliedSignature != null) {
                        String[] names = suppliedSignature.getNames();
                        return names == null ? new String[suppliedSignature.getLength()] : names;
                    }
                }
            }
            return new String[0];
        }

        @Override
        public Object getArgument(String name) {
            if (env != REnvironment.emptyEnv()) {
                assert RArguments.isRFrame(env.getFrame());
                RFunction f = RArguments.getFunction(env.getFrame());
                ArgumentsSignature signature;
                if (f != null) {
                    signature = RContext.getRRuntimeASTAccess().getArgumentsSignature(f);
                } else {
                    signature = RArguments.getSuppliedSignature(env.getFrame());
                }
                if (signature == null) {
                    return null;
                }
                for (int i = 0; i < signature.getLength(); i++) {
                    if (name.equals(signature.getName(i))) {
                        return RArguments.getArgument(env.getFrame(), i);
                    }
                }
            }
            return null;
        }

    }

    static final class GenericVariablesObject extends VariablesObject {

        private GenericVariablesObject(MaterializedFrame frame, boolean argumentsOnly) {
            super(frame == null ? new REnvEmptyFrameAccess() : new REnvTruffleFrameAccess(frame), argumentsOnly);
        }

        @Override
        protected String[] collectArgs() {
            return new String[0];
        }

        @Override
        protected Object getArgument(String name) {
            return null;
        }

    }

    @ExportLibrary(InteropLibrary.class)
    static final class ArgumentNamesObject implements TruffleObject {

        private final String[] names;

        private ArgumentNamesObject(String[] names) {
            this.names = names;
        }

        public static boolean isInstance(TruffleObject obj) {
            return obj instanceof ArgumentNamesObject;
        }

        @SuppressWarnings("static-method")
        @ExportMessage
        boolean hasArrayElements() {
            return true;
        }

        @ExportMessage
        long getArraySize() {
            return names.length;
        }

        @ExportMessage
        boolean isArrayElementReadable(long idx) {
            return idx >= 0 && idx < names.length;
        }

        @ExportMessage
        Object readArrayElement(long idx,
                        @Cached("createBinaryProfile()") ConditionProfile invalidIndex) throws InvalidArrayIndexException {
            String[] nms = this.names;
            if (invalidIndex.profile(!isArrayElementReadable(idx))) {
                throw InvalidArrayIndexException.create(idx);
            }
            int index = RRuntime.interopArrayIndexToInt(idx, this);
            return nms[index];
        }
    }
}
