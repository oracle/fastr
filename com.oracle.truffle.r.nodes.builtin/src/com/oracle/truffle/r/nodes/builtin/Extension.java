package com.oracle.truffle.r.nodes.builtin;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Interface for services, which provide {@link RBuiltinNode}'s that should loaded dynamically.
 *
 * See {@link RBuiltinPackages}
 */
public interface Extension {
	Map<Class<?>, Supplier<RBuiltinNode>> entries();
}
