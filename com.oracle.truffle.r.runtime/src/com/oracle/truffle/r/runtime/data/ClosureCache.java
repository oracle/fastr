/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.ConcurrentHashMap;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;
import com.oracle.truffle.r.runtime.nodes.RNode;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * Class that enables the caching of {@link Closure}s for certain expressions ({@link RNode}s).
 * Instance of this class is supposed to be a field in AST node, which is using the cache. The field
 * must be initialized in the constructor, but the underlying data structure (ConcurrentHashMap) is
 * initialized lazily. All methods are thread safe.
 * 
 * Closures need to be cached so that we cache the corresponding call-targets and if the expression
 * is evaluated again, we invoke it through the same call-target, which may be compiled by Truffle.
 */
public abstract class ClosureCache<K> {

    private ConcurrentHashMap<K, Closure> cache;

    public Closure getOrCreatePromiseClosure(K key) {
        return getOrCreateClosure(Closure.PROMISE_CLOSURE_WRAPPER_NAME, key);
    }

    public Closure getOrCreateLanguageClosure(K key) {
        return getOrCreateClosure(Closure.LANGUAGE_CLOSURE_WRAPPER_NAME, key);
    }

    protected abstract RBaseNode keyToNode(K key);

    @TruffleBoundary
    private Closure getOrCreateClosure(String name, K key) {
        if (key == null) {
            return null;
        }
        if (cache == null) {
            initMap();
        }
        return cache.computeIfAbsent(key, k -> Closure.create(name, keyToNode(k)));
    }

    private synchronized void initMap() {
        if (cache == null) {
            cache = new ConcurrentHashMap<>();
        }
    }

    public static final class RNodeClosureCache extends ClosureCache<RBaseNode> {
        @Override
        protected RBaseNode keyToNode(RBaseNode key) {
            return key;
        }
    }

    public static final class SymbolClosureCache extends ClosureCache<String> {
        @Override
        protected RNode keyToNode(String key) {
            return RContext.getASTBuilder().lookup(RSyntaxNode.SOURCE_UNAVAILABLE, key, false).asRNode();
        }
    }
}
