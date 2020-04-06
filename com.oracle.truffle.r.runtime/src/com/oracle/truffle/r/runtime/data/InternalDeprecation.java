/*
 * Copyright (c) 2020, 2020, Oracle and/or its affiliates. All rights reserved.
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

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks some parts of internal API that should not be used in new and should give an explanation
 * what to use instead. The long term goal is to replace all usages of given element marked with
 * this annotation and then remove the element itself. This annotation was introduced as the part of
 * the data-model and Truffle libraries refactoring. The code-base is not fully revisited, so no
 * {@link InternalDeprecation} does not mean that the given element is not legacy.
 *
 * In general, deprecated are:
 * <ul>
 * <li>All methods that read/write vector data bypassing {@link VectorDataLibrary}</li>
 * <li>All methods manipulating attributes bypassing the nodes dedicated for this purpose, e.g.,
 * {@link com.oracle.truffle.r.runtime.data.nodes.attributes.UnaryCopyAttributesNode}</li>
 * <li>All methods copying/resizing/etc. the whole vector objects bypassing
 * {@link AbstractContainerLibrary} or nodes dedicated for this purpose</li>
 * </ul>
 */
@Retention(RetentionPolicy.SOURCE)
@Target(value = {CONSTRUCTOR, FIELD, LOCAL_VARIABLE, METHOD, PACKAGE, PARAMETER, TYPE})
public @interface InternalDeprecation {
    String value();
}
