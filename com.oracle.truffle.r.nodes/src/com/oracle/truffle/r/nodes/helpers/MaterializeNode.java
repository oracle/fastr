/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.helpers;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.nodes.attributes.HasAttributesNode;
import com.oracle.truffle.r.nodes.attributes.IterableAttributeNode;
import com.oracle.truffle.r.nodes.attributes.SetAttributeNode;
import com.oracle.truffle.r.runtime.data.RAttributable;
import com.oracle.truffle.r.runtime.data.RAttributesLayout.RAttribute;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;

public abstract class MaterializeNode extends Node {

    protected static final int LIMIT = 10;

    @Child private HasAttributesNode hasAttributes;
    @Child private IterableAttributeNode attributesIt;
    @Child private SetAttributeNode setAttributeNode;

    private final boolean deep;

    protected MaterializeNode(boolean deep) {
        this.deep = deep;
        if (deep) {
            CompilerAsserts.neverPartOfCompilation();
            hasAttributes = insert(HasAttributesNode.create());
        }
    }

    public abstract Object execute(Object arg);

    @Specialization
    protected RList doList(RList vec) {
        RList materialized = materializeContents(vec);
        materializeAttributes(materialized);
        return materialized;
    }

    @Specialization(limit = "LIMIT", guards = {"vec.getClass() == cachedClass"})
    protected RAttributable doAbstractContainerCached(RAttributable vec,
                    @SuppressWarnings("unused") @Cached("vec.getClass()") Class<?> cachedClass) {
        if (vec instanceof RList) {
            return doList((RList) vec);
        } else if (vec instanceof RAbstractContainer) {
            RAbstractContainer materialized = ((RAbstractContainer) vec).materialize();
            materializeAttributes(materialized);
            return materialized;
        }
        materializeAttributes(vec);
        return vec;
    }

    @Specialization(replaces = "doAbstractContainerCached")
    protected RAttributable doAbstractContainer(RAttributable vec) {
        if (vec instanceof RList) {
            return doList((RList) vec);
        } else if (vec instanceof RAbstractContainer) {
            RAbstractContainer materialized = ((RAbstractContainer) vec).materialize();
            materializeAttributes(materialized);
            return materialized;
        }
        materializeAttributes(vec);
        return vec;
    }

    @Fallback
    protected Object doGeneric(Object o) {
        return o;
    }

    private RList materializeContents(RList list) {
        boolean changed = false;
        RList materializedContents = null;
        for (int i = 0; i < list.getLength(); i++) {
            Object element = list.getDataAt(i);
            Object materializedElem = doGenericSlowPath(element);
            if (materializedElem != element) {
                materializedContents = (RList) list.copy();
                changed = true;
            }
            if (changed && materializedElem != element) {
                materializedContents.setDataAt(i, materializedElem);
            }
        }
        if (changed) {
            return materializedContents;
        }
        return list;
    }

    private void materializeAttributes(RAttributable materialized) {
        // TODO we could further optimize by first checking for fixed/special attributes
        if (deep && hasAttributes.execute(materialized)) {
            if (attributesIt == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                attributesIt = insert(IterableAttributeNode.create());
            }
            for (RAttribute attr : attributesIt.execute(materialized)) {
                Object materializedAttr = doGenericSlowPath(attr.getValue());
                if (materializedAttr != attr.getValue()) {
                    if (setAttributeNode == null) {
                        CompilerDirectives.transferToInterpreterAndInvalidate();
                        setAttributeNode = insert(SetAttributeNode.create());
                    }
                    setAttributeNode.execute(materialized, attr.getName(), materializedAttr);
                }
            }
        }
    }

    @TruffleBoundary
    private Object doGenericSlowPath(Object element) {
        if (element instanceof RAttributable) {
            return doAbstractContainer((RAttributable) element);
        }
        return element;
    }

    public static MaterializeNode create(boolean deep) {
        return MaterializeNodeGen.create(deep);
    }

}
