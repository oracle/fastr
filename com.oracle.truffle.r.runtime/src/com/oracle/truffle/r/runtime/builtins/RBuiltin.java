/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.builtins;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.oracle.truffle.r.runtime.RDispatch;
import com.oracle.truffle.r.runtime.RVisibility;

@Retention(RetentionPolicy.RUNTIME)
public @interface RBuiltin {

    /**
     * The "kind" of the builtin.
     */
    RBuiltinKind kind();

    /**
     * The name of the builtin function in the R language.
     */
    String name();

    /**
     * The parameter names. No default, as, at a minimum, the number is required information.
     */
    String[] parameterNames();

    /**
     * Only meaningful for {@link RBuiltinKind#PRIMITIVE}, because some builtins do not have
     * arguments matched by name or they may have some special kind of matching.
     */
    ArgumentMatchingMode argumentMatchingMode() default ArgumentMatchingMode.MATCH_BY_NAME;

    /**
     * A list of aliases for {@code name()}.
     */
    String[] aliases() default {};

    /**
     * An override for {@link #name} to use in generic dispatch.
     */
    String genericName() default "";

    /**
     * Some primitives do not evaluate one or more of their arguments. This is a list of indices for
     * the non-evaluated arguments (zero based). An empty array means all arguments are evaluated.
     * N.B. The indices identify the arguments in the order they appear in the specification, i.e.,
     * after the re-ordering of named arguments. N.B. "..." is treated as a single argument for this
     * purpose and identified by its index like any other arg.
     */
    int[] nonEvalArgs() default {};

    /**
     * Do not throw error if VarArgs are to be evaluated and there is a RMissing value.
     */
    boolean allowMissingInVarArgs() default false;

    /**
     * The visibility of the output of the builtin. If the visibility is set to
     * {@link RVisibility#CUSTOM}, then it is responsibility of the execute method/specializations
     * to set the visibility in using {@code SetVisibilityNode}.
     */
    RVisibility visibility() default RVisibility.ON;

    /**
     * Determines how calls to a builtin should be dispatched, e.g., whether internal or group
     * generic dispatch should be used.
     */
    RDispatch dispatch() default RDispatch.DEFAULT;

    /**
     * The behavior defines which conditions can be expected to hold for calls to this builtin,
     * .e.g., whether repeated calls with the same arguments are expected to return the same result.
     */
    RBehavior behavior();

    /**
     * Field accesses must have at leas two arguments. The call dispatching mechanism will change
     * the second argument accordingly: if it is symbol lookup, the symbol name will be used as
     * String constant instead, if it already is a String constant it will be used as is, otherwise
     * error is raised. This special handling for field accessed is necessary, because they are also
     * internal generics and the user provided overloads only accept a String as the second
     * argument. However, there is a difference between field access with string and with a lookup
     * as the second argument. The difference is visible to code introspection with {@code quote}.
     */
    boolean isFieldAccess() default false;

    /**
     * If {@code true} the runtime should lookup the '...' symbol in the caller frame to pass it to
     * the builtin. The only example of {@code false} is builtin '~', where '...' among actual
     * arguments is interpreted only on syntax level and should not case the actual lookup of '...'
     * in the caller frame.
     */
    boolean lookupVarArgs() default true;

    /**
     * Indicates whether or not function containing a call of the form
     * <code>.Internal(name(...))</code> should trigger a split of the caller at its direct call
     * sites. <code>name</code> indicates the builtin name defined in {@link #name()}.
     */
    boolean splitCaller() default false;

    boolean alwaysSplit() default false;
}
