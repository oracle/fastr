package com.oracle.truffle.r.runtime.data;

import java.util.WeakHashMap;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.runtime.nodes.RNode;

public class LanguageClosureCache {

    private final WeakHashMap<RNode, Closure> cache = new WeakHashMap<>();

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

        Closure result = cache.get(expr);
        if (result == null) {
            result = Closure.createLanguageClosure(expr);
            cache.put(expr, result);
        }
        return result;
    }

}
