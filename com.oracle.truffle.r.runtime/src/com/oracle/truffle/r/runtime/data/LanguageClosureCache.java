package com.oracle.truffle.r.runtime.data;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.nodes.RNode;

public class LanguageClosureCache {

    private final WeakHashMap<RNode, WeakReference<Closure>> cache = new WeakHashMap<>();

    /**
     * @param expr
     * @return A {@link Closure} representing the given {@link RNode}. If expr is <code>null</code>
     *         <code>null</code> is returned.
     */
    @TruffleBoundary
    public Closure getOrCreateLanguageClosure(RNode expr) {
        if (expr == null) {
            return null;
        }

        WeakReference<Closure> weakReference = cache.get(expr);
        Closure result = weakReference != null ? weakReference.get() : null;
        if (result == null) {
            result = Closure.createLanguageClosure(expr);
            cache.put(expr, new WeakReference<>(result));
        }
        return result;
    }

}
