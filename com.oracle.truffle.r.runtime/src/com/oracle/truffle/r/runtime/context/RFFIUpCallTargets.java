/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.runtime.context;

import com.oracle.truffle.api.RootCallTarget;

// Checkstyle: stop field name check
/**
 * This is manually maintained class that must contain one field for each of the RFFI upcalls that
 * need a CallTarget (see {@code RFFIUpCallNode.needsCallTarget}).
 */
public final class RFFIUpCallTargets {

    RFFIUpCallTargets() {

    }

    public volatile RootCallTarget CoerceVectorNode;

    public volatile RootCallTarget AsIntegerNode;

    public volatile RootCallTarget AsRealNode;

    public volatile RootCallTarget AsLogicalNode;

    public volatile RootCallTarget AsCharNode;

    public volatile RootCallTarget RDoNewObjectNode;

    public volatile RootCallTarget ATTRIB;

    public volatile RootCallTarget GetAttrib;

    public volatile RootCallTarget RfSetAttribNode;

    public volatile RootCallTarget LengthGetsNode;

    public volatile RootCallTarget RfEvalNode;

    public volatile RootCallTarget TryRfEvalNode;

    public volatile RootCallTarget RDoSlotNode;

    public volatile RootCallTarget RandFunction3Node;

    public volatile RootCallTarget RfRMultinomNode;

    public volatile RootCallTarget BesselIExNode;

    public volatile RootCallTarget BesselJExNode;

    public volatile RootCallTarget BesselKExNode;

    public volatile RootCallTarget BesselYExNode;

    public volatile RootCallTarget NamesGetsNode;

    public volatile RootCallTarget VectorToPairListNode;

    public volatile RootCallTarget AsCharacterFactor;

    public volatile RootCallTarget RHasSlotNode;

    public volatile RootCallTarget OctSizeNode;

    public volatile RootCallTarget RForceAndCallNode;

    public volatile RootCallTarget AsS4;

    public volatile RootCallTarget Match5UpCallNode;

    public volatile RootCallTarget RfAllocArrayNode;
}
