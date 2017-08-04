/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.r.nodes.access.WriteVariableNode;
import com.oracle.truffle.r.nodes.access.WriteVariableNode.Mode;
import com.oracle.truffle.r.nodes.access.vector.ElementAccessMode;
import com.oracle.truffle.r.nodes.access.vector.ExtractVectorNode;
import com.oracle.truffle.r.nodes.attributes.SpecialAttributesFunctions.GetNamesAttributeNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.base.MapplyNodeGen.MapplyInternalNodeGen;
import com.oracle.truffle.r.nodes.control.RLengthNode;
import com.oracle.truffle.r.nodes.function.RCallBaseNode;
import com.oracle.truffle.r.nodes.function.RCallNode;
import com.oracle.truffle.r.nodes.function.call.RExplicitCallNode;
import com.oracle.truffle.r.runtime.AnonymousFrameVariable;
import com.oracle.truffle.r.runtime.ArgumentsSignature;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.context.RContext;
import com.oracle.truffle.r.runtime.data.RArgsValuesAndNames;
import com.oracle.truffle.r.runtime.data.RDataFactory;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RLogical;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractListVector;
import com.oracle.truffle.r.runtime.nodes.InternalRSyntaxNodeChildren;
import com.oracle.truffle.r.runtime.nodes.RSyntaxNode;

/**
 * Multivariate lapply. Essentially invokes
 * {@code fun(dots[0][X], dots[1][X], , dots[N][X], MoreArgs)} for {@code X=1..M} where {@code M} is
 * the longest vector, with the usual recycling rule.
 */
@RBuiltin(name = "mapply", kind = INTERNAL, parameterNames = {"FUN", "dots", "MoreArgs"}, splitCaller = true, behavior = COMPLEX)
public abstract class Mapply extends RBuiltinNode.Arg3 {

    static {
        Casts casts = new Casts(Mapply.class);
        // let's assume that mapply internal is never called directly, otherwise all hell ensues -
        // even in GNU R .Internal(mapply(rep, 1:4, NULL)) causes a segfault
        casts.arg("FUN").mustBe(instanceOf(RFunction.class));
        casts.arg("dots").mustBe(instanceOf(RAbstractListVector.class));
        // if we could map to an empty list, we could get rid of an additional specialization
        casts.arg("MoreArgs").allowNull().mustBe(instanceOf(RAbstractListVector.class));
    }

    protected static final class ElementNode extends Node {
        @Child private Length lengthNode;
        @Child private ExtractVectorNode extractNode;
        @Child private WriteVariableNode writeVectorElementNode;
        private final String vectorElementName;
        private final String argName;

        private ElementNode(String vectorElementName, String argName) {
            // the name is a hack to treat ReadVariableNode-s as syntax nodes
            this.vectorElementName = "*" + AnonymousFrameVariable.create(vectorElementName);
            this.lengthNode = insert(LengthNodeGen.create());
            this.extractNode = insert(ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, false));
            this.writeVectorElementNode = insert(WriteVariableNode.createAnonymous(this.vectorElementName, Mode.REGULAR, null));
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

    @Specialization
    protected Object mApply(VirtualFrame frame, RFunction fun, RAbstractListVector dots, @SuppressWarnings("unused") RNull moreArgs) {
        return mApply(frame, fun, dots, RDataFactory.createList());
    }

    public abstract static class MapplyInternalNode extends Node implements InternalRSyntaxNodeChildren {

        private static final String VECTOR_ELEMENT_PREFIX = "MAPPLY_VEC_ELEM_";
        private static final ArgumentsSignature I_INDEX = ArgumentsSignature.get("i");
        private static final RArgsValuesAndNames[] INDEX_CACHE = new RArgsValuesAndNames[32];

        static {
            for (int i = 0; i < INDEX_CACHE.length; i++) {
                INDEX_CACHE[i] = new RArgsValuesAndNames(new Object[]{i + 1}, I_INDEX);
            }
        }

        @Child private GetNamesAttributeNode getNamesDots = GetNamesAttributeNode.create();
        @Child private GetNamesAttributeNode getNamesMoreArgs = GetNamesAttributeNode.create();

        private final BranchProfile nonPerfectMatch = BranchProfile.create();

        public abstract Object[] execute(VirtualFrame frame, RAbstractListVector dots, RFunction function, RAbstractListVector additionalArguments);

        private static Object getVecElement(VirtualFrame frame, RAbstractListVector dots, int i, int listIndex, int[] lengths, ExtractVectorNode extractNode) {
            return extractNode.apply(frame, dots.getDataAt(listIndex), new Object[]{i % lengths[listIndex] + 1}, RLogical.TRUE, RLogical.TRUE);
        }

        @Specialization(limit = "5", guards = {"dots.getLength() == dotsLength", "moreArgs.getLength() == moreArgsLength",
                        "sameNames(dots, cachedDotsNames)", "sameNames(moreArgs, cachedMoreArgsNames)"})
        protected Object[] cachedMApply(VirtualFrame frame, RAbstractListVector dots, RFunction function, RAbstractListVector moreArgs,
                        @Cached("dots.getLength()") int dotsLength,
                        @Cached("moreArgs.getLength()") int moreArgsLength,
                        @SuppressWarnings("unused") @Cached(value = "extractNames(dots)", dimensions = 1) String[] cachedDotsNames,
                        @SuppressWarnings("unused") @Cached(value = "extractNames(moreArgs)", dimensions = 1) String[] cachedMoreArgsNames,
                        @Cached("createElementNodeArray(dotsLength, moreArgsLength, cachedDotsNames, cachedMoreArgsNames)") ElementNode[] cachedElementNodeArray,
                        @Cached("createCallNode(cachedElementNodeArray)") RCallBaseNode callNode) {
            int[] lengths = new int[dotsLength];
            int maxLength = getDotsLengths(frame, dots, dotsLength, cachedElementNodeArray, lengths);
            storeAdditionalArguments(frame, moreArgs, dotsLength, moreArgsLength, cachedElementNodeArray);
            Object[] result = new Object[maxLength];
            for (int i = 0; i < maxLength; i++) {
                /* Evaluate and store the arguments */
                prepareElements(frame, dots, dotsLength, cachedElementNodeArray, lengths, i);
                /* Now call the function */
                result[i] = callNode.execute(frame, function);
            }
            return result;
        }

        @ExplodeLoop
        private static void prepareElements(VirtualFrame frame, RAbstractListVector dots, int dotsLength, ElementNode[] cachedElementNodeArray, int[] lengths, int i) {
            for (int listIndex = 0; listIndex < dotsLength; listIndex++) {
                Object vecElement = getVecElement(frame, dots, i, listIndex, lengths, cachedElementNodeArray[listIndex].extractNode);
                cachedElementNodeArray[listIndex].writeVectorElementNode.execute(frame, vecElement);
            }
        }

        @ExplodeLoop
        private static void storeAdditionalArguments(VirtualFrame frame, RAbstractListVector moreArgs, int dotsLength, int moreArgsLength, ElementNode[] cachedElementNodeArray) {
            for (int listIndex = dotsLength; listIndex < dotsLength + moreArgsLength; listIndex++) {
                // store additional arguments
                cachedElementNodeArray[listIndex].writeVectorElementNode.execute(frame, moreArgs.getDataAt(listIndex - dotsLength));
            }
        }

        @ExplodeLoop
        private static int getDotsLengths(VirtualFrame frame, RAbstractListVector dots, int dotsLength, ElementNode[] cachedElementNodeArray, int[] lengths) {
            int maxLength = -1;
            for (int i = 0; i < dotsLength; i++) {
                int length = cachedElementNodeArray[i].lengthNode.executeInt(frame, dots.getDataAt(i));
                if (length > maxLength) {
                    maxLength = length;
                }
                lengths[i] = length;
            }
            return maxLength;
        }

        protected static String[] extractNames(RAbstractListVector list) {
            CompilerAsserts.neverPartOfCompilation();
            RStringVector names = list.getNames();
            return names == null ? null : names.getDataCopy();
        }

        @Specialization(replaces = "cachedMApply")
        protected Object[] mApply(VirtualFrame frame, RAbstractListVector dots, RFunction function, RAbstractListVector moreArgs,
                        @Cached("create()") RLengthNode lengthNode,
                        @Cached("createExtractNode()") ExtractVectorNode extractNode,
                        @Cached("create()") RExplicitCallNode callNode) {
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
            RStringVector dotsNames = getNamesDots.getNames(dots);
            if (dotsNames != null) {
                for (int listIndex = 0; listIndex < dotsLength; listIndex++) {
                    names[listIndex] = dotsNames.getDataAt(listIndex).isEmpty() ? null : dotsNames.getDataAt(listIndex);
                }
            }
            RStringVector moreArgsNames = getNamesMoreArgs.getNames(moreArgs);
            for (int listIndex = dotsLength; listIndex < dotsLength + moreArgsLength; listIndex++) {
                values[listIndex] = moreArgs.getDataAt(listIndex - dotsLength);
                names[listIndex] = moreArgsNames == null ? null : (moreArgsNames.getDataAt(listIndex - dotsLength).isEmpty() ? null : moreArgsNames.getDataAt(listIndex - dotsLength));
            }
            ArgumentsSignature signature = ArgumentsSignature.get(names);
            Object[] result = new Object[maxLength];
            for (int i = 0; i < maxLength; i++) {
                /* Evaluate and store the arguments */
                for (int listIndex = 0; listIndex < dotsLength; listIndex++) {
                    Object vecElement = getVecElement(frame, dots, i, listIndex, lengths, extractNode);
                    values[listIndex] = vecElement;
                }
                /* Now call the function */
                result[i] = callNode.execute(frame, function, new RArgsValuesAndNames(values, signature));
            }
            return result;
        }

        /**
         * Creates the {@link RCallNode} for this target.
         */
        protected RCallBaseNode createCallNode(ElementNode[] elementNodeArray) {
            CompilerAsserts.neverPartOfCompilation();
            RSyntaxNode[] syntaxNodes = new RSyntaxNode[elementNodeArray.length];
            String[] names = new String[elementNodeArray.length];
            for (int i = 0; i < syntaxNodes.length; i++) {
                syntaxNodes[i] = RContext.getASTBuilder().lookup(RSyntaxNode.LAZY_DEPARSE, elementNodeArray[i].vectorElementName, false);
                names[i] = elementNodeArray[i].argName;
            }
            // Errors can be thrown from the modified call so a SourceSection is required
            return RCallNode.createCall(Lapply.createCallSourceSection(), null, ArgumentsSignature.get(names), syntaxNodes);
        }

        protected ElementNode[] createElementNodeArray(int dotsLength, int moreArgsLength, String[] cachedDotsNames, String[] cachedMoreArgsNames) {
            int length = dotsLength + moreArgsLength;
            ElementNode[] elementNodes = new ElementNode[length];
            for (int i = 0; i < dotsLength; i++) {
                elementNodes[i] = insert(new ElementNode(VECTOR_ELEMENT_PREFIX + (i + 1), cachedDotsNames == null ? null : (cachedDotsNames[i].isEmpty() ? null : cachedDotsNames[i])));
            }
            for (int i = 0; i < moreArgsLength; i++) {
                elementNodes[i + dotsLength] = insert(
                                new ElementNode(VECTOR_ELEMENT_PREFIX + (i + 1 + dotsLength), cachedMoreArgsNames == null ? null : cachedMoreArgsNames[i].isEmpty() ? null : cachedMoreArgsNames[i]));
            }
            return elementNodes;
        }

        protected ExtractVectorNode createExtractNode() {
            return ExtractVectorNode.create(ElementAccessMode.SUBSCRIPT, false);
        }

        protected boolean sameNames(RAbstractListVector list, String[] cachedNames) {
            RStringVector listNames = getNamesDots.getNames(list);
            if (listNames == null && cachedNames == null) {
                return true;
            } else if (listNames == null || cachedNames == null) {
                return false;
            } else {
                for (int i = 0; i < cachedNames.length; i++) {
                    String name = listNames.getDataAt(i);
                    String cachedName = cachedNames[i];
                    if (name == cachedName) {
                        continue;
                    } else {
                        nonPerfectMatch.enter();
                        if (name == null || cachedName == null) {
                            return false;
                        } else if (!equals(name, cachedName)) {
                            return false;
                        }
                    }
                }
                return true;
            }
        }

        @TruffleBoundary
        private static boolean equals(String name, String cachedName) {
            return name.equals(cachedName);
        }
    }
}
