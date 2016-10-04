/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.INTERNAL;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.nodes.access.FrameSlotNode;
import com.oracle.truffle.r.nodes.access.WriteVariableNode;
import com.oracle.truffle.r.nodes.access.WriteVariableNode.Mode;
import com.oracle.truffle.r.nodes.access.variables.ReadVariableNode;
import com.oracle.truffle.r.nodes.builtin.CastBuilder;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.MapplyNodeGen.MapplyInternalNodeGen;
import com.oracle.truffle.r.nodes.builtin.base.infix.Subscript;
import com.oracle.truffle.r.nodes.builtin.base.infix.SubscriptNodeGen;
import com.oracle.truffle.r.nodes.control.RLengthNode;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.runtime.AnonymousFrameVariable;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.RInternalError;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RAttributeProfiles;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLogicalVector;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.nodes.InternalRSyntaxNodeChildren;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * Multivariate lapply. Essentially invokes
 * {@code fun(dots[0][X], dots[1][X], , dots[N][X], MoreArgs)} for {@code X=1..M} where {@code M} is
 * the longest vector, with the usual recycling rule.
 */
@RBuiltin(name = "mapply", kind = INTERNAL, parameterNames = {"FUN", "dots", "MoreArgs"}, splitCaller = true, behavior = COMPLEX)
public abstract class Mapply extends RBuiltinNode {

    @Override
    protected void createCasts(CastBuilder casts) {
        // let's assume that mapply internal is never called directly, otherwise all hell ensues -
        // even in GNU R .Internal(mapply(rep, 1:4, NULL)) causes a segfault
        casts.arg("FUN").mustBe(instanceOf(RFunction.class));
        casts.arg("dots").mustBe(instanceOf(RAbstractListVector.class));
        // if we could map to an empty list, we could get rid of an additional specialization
        casts.arg("MoreArgs").allowNull().mustBe(instanceOf(RAbstractListVector.class));
    }

    protected static final class ElementNode extends Node {
        @Child private Length lengthNode;
        @Child private Subscript indexedLoadNode;
        @Child private WriteVariableNode writeVectorElementNode;
        private final String vectorElementName;
        private final String argName;

        private ElementNode(String vectorElementName, String argName) {
            // the name is a hack to treat ReadVariableNode-s as syntax nodes
            this.vectorElementName = "*" + AnonymousFrameVariable.create(vectorElementName);
            this.lengthNode = insert(LengthNodeGen.create(null));
            this.indexedLoadNode = insert(SubscriptNodeGen.create(null));
            this.writeVectorElementNode = insert(WriteVariableNode.createAnonymous(this.vectorElementName, null, Mode.REGULAR));
            this.argName = argName;
        }
    }

    @Child private MapplyInternalNode mapply = MapplyInternalNodeGen.create();

    @Specialization
    protected Object mApply(VirtualFrame frame, RFunction fun, RAbstractListVector dots, RAbstractListVector moreArgs) {
        Object[] result = mapply.execute(frame, dots, fun, moreArgs);
        // set here else it gets overridden by the iterator evaluation
        return RDataFactory.createList(result);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object mApply(VirtualFrame frame, RFunction fun, RAbstractListVector dots, RNull moreArgs) {
        return mApply(frame, fun, dots, RDataFactory.createList());
    }

    public abstract static class MapplyInternalNode extends Node implements InternalRSyntaxNodeChildren {

        private static final String VECTOR_ELEMENT_PREFIX = "MAPPLY_VEC_ELEM_";
        private static final RLogicalVector DROP = RDataFactory.createLogicalVectorFromScalar(true);
        private static final RLogicalVector EXACT = RDataFactory.createLogicalVectorFromScalar(true);
        private static final ArgumentsSignature I_INDEX = ArgumentsSignature.get("i");
        private static final RArgsValuesAndNames[] INDEX_CACHE = new RArgsValuesAndNames[32];

        static {
            for (int i = 0; i < INDEX_CACHE.length; i++) {
                INDEX_CACHE[i] = new RArgsValuesAndNames(new Object[]{i + 1}, I_INDEX);
            }
        }

        private final RAttributeProfiles attrProfiles = RAttributeProfiles.create();

        public abstract Object[] execute(VirtualFrame frame, RAbstractListVector dots, RFunction function, RAbstractListVector additionalArguments);

        private static Object getVecElement(VirtualFrame frame, RAbstractListVector dots, int i, int listIndex, int[] lengths, Subscript indexedLoadNode) {
            Object listElem = dots.getDataAt(listIndex);
            RAbstractContainer vec = null;
            if (listElem instanceof RAbstractContainer) {
                vec = (RAbstractContainer) listElem;
            } else {
                // TODO scalar types are a nuisance!
                if (listElem instanceof String) {
                    vec = RDataFactory.createStringVectorFromScalar((String) listElem);
                } else if (listElem instanceof Integer) {
                    vec = RDataFactory.createIntVectorFromScalar((int) listElem);
                } else if (listElem instanceof Double) {
                    vec = RDataFactory.createDoubleVectorFromScalar((double) listElem);
                } else {
                    throw RInternalError.unimplemented();
                }
            }

            int adjIndex = i % lengths[listIndex];
            RArgsValuesAndNames indexArg;
            if (adjIndex < INDEX_CACHE.length) {
                indexArg = INDEX_CACHE[adjIndex];
            } else {
                indexArg = new RArgsValuesAndNames(new Object[]{adjIndex + 1}, I_INDEX);
            }
            return indexedLoadNode.execute(frame, vec, indexArg, EXACT, DROP);

        }

        @SuppressWarnings("unused")
        @Specialization(limit = "5", guards = {"dots.getLength() == cachedDots.getLength()",
                        "moreArgs.getLength() == cachedMoreArgs.getLength()", "sameNames(dots, cachedDots)",
                        "sameNames(moreArgs, cachedMoreArgs)"})
        protected Object[] cachedMApply(VirtualFrame frame, RAbstractListVector dots, RFunction function, RAbstractListVector moreArgs,
                        @Cached("dots") RAbstractListVector cachedDots,
                        @Cached("moreArgs") RAbstractListVector cachedMoreArgs,
                        @Cached("createElementNodeArray(cachedDots, cachedMoreArgs)") ElementNode[] cachedElementNodeArray,
                        @Cached("createCallNode(cachedElementNodeArray)") RCallNode callNode) {
            int dotsLength = dots.getLength();
            int moreArgsLength = moreArgs.getLength();
            int[] lengths = new int[dotsLength];
            int maxLength = -1;
            for (int i = 0; i < dotsLength; i++) {
                int length = cachedElementNodeArray[i].lengthNode.executeInt(frame, dots.getDataAt(i));
                if (length > maxLength) {
                    maxLength = length;
                }
                lengths[i] = length;
            }
            for (int listIndex = dotsLength; listIndex < dotsLength + moreArgsLength; listIndex++) {
                // store additional arguments
                cachedElementNodeArray[listIndex].writeVectorElementNode.execute(frame,
                                moreArgs.getDataAt(listIndex - dotsLength));
            }
            Object[] result = new Object[maxLength];
            for (int i = 0; i < maxLength; i++) {
                /* Evaluate and store the arguments */
                for (int listIndex = 0; listIndex < dotsLength; listIndex++) {
                    Object vecElement = getVecElement(frame, dots, i, listIndex, lengths, cachedElementNodeArray[listIndex].indexedLoadNode);
                    cachedElementNodeArray[listIndex].writeVectorElementNode.execute(frame, vecElement);
                }
                /* Now call the function */
                result[i] = callNode.execute(frame, function);
            }
            return result;
        }

        @Specialization(contains = "cachedMApply")
        protected Object[] mApply(VirtualFrame frame, RAbstractListVector dots, RFunction function, RAbstractListVector moreArgs,
                        @SuppressWarnings("unused") @Cached("createArgsIdentifier()") Object argsIdentifier,
                        @Cached("createFrameSlotNode(argsIdentifier)") FrameSlotNode slotNode,
                        @Cached("create()") RLengthNode lengthNode,
                        @Cached("createIndexedLoadNode()") Subscript indexedLoadNode,
                        @Cached("createExplicitCallNode(argsIdentifier)") RCallNode callNode) {
            int dotsLength = dots.getLength();
            int moreArgsLength = moreArgs.getLength();
            int[] lengths = new int[dotsLength];
            int maxLength = -1;
            for (int i = 0; i < dotsLength; i++) {
                int length = lengthNode.executeInteger(frame, dots.getDataAt(i));
                if (length > maxLength) {
                    maxLength = length;
                }
                lengths[i] = length;
            }
            Object[] values = new Object[dotsLength + moreArgsLength];
            String[] names = new String[dotsLength + moreArgsLength];
            RStringVector dotsNames = dots.getNames(attrProfiles);
            if (dotsNames != null) {
                for (int listIndex = 0; listIndex < dotsLength; listIndex++) {
                    names[listIndex] = dotsNames.getDataAt(listIndex).isEmpty() ? null : dotsNames.getDataAt(listIndex);
                }
            }
            RStringVector moreArgsNames = moreArgs.getNames(attrProfiles);
            for (int listIndex = dotsLength; listIndex < dotsLength + moreArgsLength; listIndex++) {
                values[listIndex] = moreArgs.getDataAt(listIndex - dotsLength);
                names[listIndex] = moreArgsNames == null ? null : (moreArgsNames.getDataAt(listIndex - dotsLength).isEmpty() ? null : moreArgsNames.getDataAt(listIndex - dotsLength));
            }
            ArgumentsSignature signature = ArgumentsSignature.get(names);
            Object[] result = new Object[maxLength];
            for (int i = 0; i < maxLength; i++) {
                /* Evaluate and store the arguments */
                for (int listIndex = 0; listIndex < dotsLength; listIndex++) {
                    Object vecElement = getVecElement(frame, dots, i, listIndex, lengths, indexedLoadNode);
                    values[listIndex] = vecElement;
                }
                /* Now call the function */
                FrameSlot frameSlot = slotNode.executeFrameSlot(frame);
                frame.setObject(frameSlot, new RArgsValuesAndNames(values, signature));
                result[i] = callNode.execute(frame, function);
            }
            return result;
        }

        /**
         * Creates the {@link RCallNode} for this target.
         */
        protected RCallNode createCallNode(ElementNode[] elementNodeArray) {
            CompilerAsserts.neverPartOfCompilation();
            RSyntaxNode[] syntaxNodes = new RSyntaxNode[elementNodeArray.length];
            String[] names = new String[elementNodeArray.length];
            for (int i = 0; i < syntaxNodes.length; i++) {
                syntaxNodes[i] = ReadVariableNode.create(elementNodeArray[i].vectorElementName).asRSyntaxNode();
                names[i] = elementNodeArray[i].argName;
            }
            // Errors can be thrown from the modified call so a SourceSection is required
            return RCallNode.createCall(Lapply.createCallSourceSection(), null, ArgumentsSignature.get(names), syntaxNodes);
        }

        protected ElementNode[] createElementNodeArray(RAbstractListVector dots, RAbstractListVector moreArgs) {
            int length = dots.getLength() + moreArgs.getLength();
            ElementNode[] elementNodes = new ElementNode[length];
            RStringVector dotsNames = dots.getNames(attrProfiles);
            for (int i = 0; i < dots.getLength(); i++) {
                elementNodes[i] = insert(new ElementNode(VECTOR_ELEMENT_PREFIX + (i + 1), dotsNames == null ? null : (dotsNames.getDataAt(i).isEmpty() ? null : dotsNames.getDataAt(i))));
            }
            RStringVector moreArgsNames = moreArgs.getNames(attrProfiles);
            for (int i = dots.getLength(); i < dots.getLength() + moreArgs.getLength(); i++) {
                elementNodes[i] = insert(new ElementNode(VECTOR_ELEMENT_PREFIX + (i + 1),
                                moreArgsNames == null ? null : moreArgsNames.getDataAt(i - dots.getLength()).isEmpty() ? null : moreArgsNames.getDataAt(i - dots.getLength())));
            }
            return elementNodes;
        }

        protected Object createArgsIdentifier() {
            return new Object();
        }

        protected FrameSlotNode createFrameSlotNode(Object argsIdentifier) {
            return FrameSlotNode.createTemp(argsIdentifier, true);
        }

        protected RCallNode createExplicitCallNode(Object argsIdentifier) {
            return RCallNode.createExplicitCall(argsIdentifier);
        }

        protected Subscript createIndexedLoadNode() {
            return SubscriptNodeGen.create(null);
        }

        protected boolean sameNames(RAbstractListVector list, RAbstractListVector cachedList) {
            if (list.getNames(attrProfiles) == null && cachedList.getNames(attrProfiles) == null) {
                return true;
            } else if (list.getNames(attrProfiles) == null || cachedList.getNames(attrProfiles) == null) {
                return false;
            } else {
                for (int i = 0; i < list.getLength(); i++) {
                    String name = list.getNames(attrProfiles).getDataAt(i);
                    String cachedName = cachedList.getNames(attrProfiles).getDataAt(i);

                    if (name == cachedName) {
                        continue;
                    } else if (name == null || cachedName == null) {
                        return false;
                    } else if (!(name.equals(cachedName))) {
                        return false;
                    }
                }
                return true;
            }
        }

    }
}
