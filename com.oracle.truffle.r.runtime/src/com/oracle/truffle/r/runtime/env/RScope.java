/*
 * Copyright (c) 2021, 2022, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.InvalidArrayIndexException;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RTruffleBaseObject;
import com.oracle.truffle.r.runtime.env.REnvironment.PutException;
import com.oracle.truffle.r.runtime.env.frame.REnvFrameAccess;
import com.oracle.truffle.r.runtime.interop.Foreign2R;
import com.oracle.truffle.r.runtime.interop.R2Foreign;

@ExportLibrary(InteropLibrary.class)
public class RScope extends RTruffleBaseObject {

    private final REnvironment env;
    private final REnvFrameAccess frameAccess;

    /**
     * If RootNode is available we use it for function name and source section, but we should be
     * able to get this info also from the environment.
     */
    private final RootNode rootNode;

    /**
     * Chain of scopes. The first explicitly constructed scope creates this chain. All the scopes in
     * this chain hold onto this array and remember their index within it as
     * {@link #currentScopeOffset}.
     */
    private final RScope[] scopesChain;
    private final int currentScopeOffset;

    /**
     * Cached collections with the local names of the current scope, initialized lazily.
     */
    private volatile List<String> currentNames;
    private volatile List<String> currentNamesOnlyPublic;

    public RScope(REnvironment env, REnvFrameAccess frameAccess, RootNode rootNode) {
        assert frameAccess != null;
        this.env = env;
        this.rootNode = rootNode;
        this.frameAccess = frameAccess;
        this.scopesChain = getScopesChain(rootNode != null);
        this.currentScopeOffset = 0;
    }

    public RScope(REnvironment env, REnvFrameAccess frameAccess, RScope[] scopesChain, int currentScopeOffset) {
        assert frameAccess != null;
        assert scopesChain != null;
        assert currentScopeOffset < scopesChain.length && currentScopeOffset >= 0;
        this.env = env;
        this.rootNode = null;
        this.frameAccess = frameAccess;
        this.scopesChain = scopesChain;
        this.currentScopeOffset = currentScopeOffset;
    }

    @ExportMessage
    public boolean isScope() {
        return true;
    }

    @ExportMessage
    @SuppressWarnings("static-method")
    final boolean hasMembers() {
        return true;
    }

    @ExportMessage
    @TruffleBoundary
    public RScopeMembers getMembers(boolean includeInternal) {
        return new RScopeMembers(includeInternal);
    }

    @ExportMessage
    public boolean isMemberReadable(String member) {
        return exists(member);
    }

    @ExportMessage
    boolean isMemberInsertable(String member) {
        return !exists(member);
    }

    @ExportMessage
    @TruffleBoundary
    boolean isMemberModifiable(String member) {
        for (int i = currentScopeOffset; i < scopesChain.length; i++) {
            if (scopesChain[i].getCurrentNames(true).contains(member)) {
                return !frameAccess.bindingIsLocked(member);
            }
        }
        return false;
    }

    @ExportMessage
    boolean isMemberInvocable(String member) {
        try {
            return readRawMember(member) instanceof RFunction;
        } catch (UnknownIdentifierException e) {
            return false;
        }
    }

    @ExportMessage
    public boolean hasMemberReadSideEffects(String member) {
        return isActiveBinding(member);
    }

    @ExportMessage
    public boolean hasMemberWriteSideEffects(String member) {
        return isActiveBinding(member);
    }

    @TruffleBoundary
    private boolean exists(String member) {
        for (int i = currentScopeOffset; i < scopesChain.length; i++) {
            if (scopesChain[i].getCurrentNames(true).contains(member)) {
                return true;
            }
        }
        return false;
    }

    @TruffleBoundary
    private boolean isActiveBinding(String member) {
        for (int i = currentScopeOffset; i < scopesChain.length; i++) {
            REnvFrameAccess access = scopesChain[i].frameAccess;
            if (access.isActiveBinding(member)) {
                return true;
            }
            Object value = access.get(member);
            if (value != null) {
                return false;
            }
        }
        return false;
    }

    @ExportMessage
    Object readMember(String member,
                    @Cached R2Foreign r2Foreign) throws UnknownIdentifierException {
        return r2Foreign.convert(readRawMember(member));
    }

    @TruffleBoundary
    Object readRawMember(String member) throws UnknownIdentifierException {
        for (int i = currentScopeOffset; i < scopesChain.length; i++) {
            Object value = scopesChain[i].frameAccess.get(member);
            if (value != null) {
                return value;
            }
        }
        throw UnknownIdentifierException.create(member);
    }

    @ExportMessage
    @TruffleBoundary
    final void writeMember(String member, Object value,
                    @Cached Foreign2R foreign2R) throws UnsupportedMessageException {
        for (int i = currentScopeOffset; i < scopesChain.length; i++) {
            Object existingValue = scopesChain[i].frameAccess.get(member);
            if (existingValue != null) {
                try {
                    // Note: frame access takes care of active bindings
                    scopesChain[i].frameAccess.put(member, foreign2R.convert(value));
                } catch (PutException e) {
                    // locked binding, the member should not have been modifiable/insertable
                    throw UnsupportedMessageException.create();
                }
            }
        }
        // Not found. By default, we'll insert into the current scope
        try {
            frameAccess.put(member, foreign2R.convert(value));
        } catch (PutException e) {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    Object invokeMember(String identifier, Object[] args,
                    @Cached() RFunction.ExplicitCall c,
                    @Cached("createBinaryProfile()") ConditionProfile isFunction) throws UnsupportedMessageException, UnknownIdentifierException {
        Object value = readRawMember(identifier);
        if (isFunction.profile(value instanceof RFunction)) {
            // TODO: we should translate R error re incorrect arguments count to interop exception
            return c.execute((RFunction) value, args);
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    public boolean hasScopeParent() {
        return currentScopeOffset + 1 < scopesChain.length;
    }

    @ExportMessage
    public Object getScopeParent() throws UnsupportedMessageException {
        if (!hasScopeParent()) {
            throw UnsupportedMessageException.create();
        }
        return scopesChain[currentScopeOffset + 1];
    }

    @ExportMessage
    @Override
    public boolean hasSourceLocation() {
        return rootNode != null;
    }

    @ExportMessage
    @Override
    @TruffleBoundary
    public SourceSection getSourceLocation() throws UnsupportedMessageException {
        if (rootNode != null) {
            return rootNode.getSourceSection();
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    @Override
    @TruffleBoundary
    public Object toDisplayString(@SuppressWarnings("unused") boolean allowSideEffects) {
        if (rootNode != null) {
            String rootName = rootNode.getName();
            if (!"".equals(rootName)) {
                return nameForFunctionEnv(rootName);
            }
        }
        if (env != null) {
            RFunction function = RArguments.getFunction(env.getFrame());
            if (function != null) {
                return nameForFunctionEnv(function.getName());
            } else {
                String name = env.getName();
                return "explicit environment" + (name != null ? ": " + name : "");
            }
        }
        return "unknown R scope";
    }

    private static String nameForFunctionEnv(String funName) {
        return "function environment" + (funName != null ? " for function " + funName : "");
    }

    private RScope[] getScopesChain(boolean localScopes) {
        if (env == null) {
            return new RScope[0];
        }

        int parentsCount = 0;
        REnvironment currentEnv = env;
        while (currentEnv != null && acceptEnv(currentEnv, localScopes)) {
            currentEnv = currentEnv.getParent();
            parentsCount++;
        }

        RScope[] result = new RScope[parentsCount];
        currentEnv = env;
        for (int i = 0; i < result.length; i++) {
            result[i] = new RScope(currentEnv, currentEnv.getFrameAccess(), result, i);
            currentEnv = currentEnv.getParent();
        }
        return result;
    }

    @TruffleBoundary
    private static boolean acceptEnv(REnvironment env, boolean localScopes) {
        return env != REnvironment.emptyEnv() && !(localScopes && env == REnvironment.globalEnv());
    }

    private List<String> getCurrentNames(boolean includeInternal) {
        CompilerAsserts.neverPartOfCompilation();
        if (includeInternal) {
            if (currentNames == null) {
                currentNames = Arrays.asList(frameAccess.ls(true, null, false).getReadonlyStringData());
            }
            return currentNames;
        } else {
            if (currentNamesOnlyPublic == null) {
                currentNamesOnlyPublic = Arrays.asList(frameAccess.ls(false, null, false).getReadonlyStringData());
            }
            return currentNamesOnlyPublic;
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public final class RScopeMembers extends RTruffleBaseObject {

        private final boolean includeInternal;
        private final int size;

        public RScopeMembers(boolean includeInternal) {
            this.includeInternal = includeInternal;
            int sizeLocal = 0;
            for (int i = currentScopeOffset; i < scopesChain.length; i++) {
                sizeLocal += scopesChain[i].getCurrentNames(includeInternal).size();
            }
            this.size = sizeLocal;
        }

        @ExportMessage
        boolean hasArrayElements() {
            return true;
        }

        @ExportMessage
        public long getArraySize() {
            return size;
        }

        @ExportMessage
        boolean isArrayElementReadable(long index) {
            return index < size && index >= 0;
        }

        @ExportMessage
        @TruffleBoundary
        public String readArrayElement(long indexL) throws InvalidArrayIndexException {
            int currentIndex = 0;
            int index = (int) indexL;
            for (int i = currentScopeOffset; i < scopesChain.length; i++) {
                List<String> names = scopesChain[i].getCurrentNames(includeInternal);
                if (currentIndex + names.size() > index) {
                    return names.get(index - currentIndex);
                }
                currentIndex += names.size();
            }
            throw InvalidArrayIndexException.create(indexL);
        }
    }
}
