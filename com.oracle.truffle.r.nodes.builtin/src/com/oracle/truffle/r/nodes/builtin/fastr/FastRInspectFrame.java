/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
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

package com.oracle.truffle.r.nodes.builtin.fastr;

import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.constant;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.emptyStringVector;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.instanceOf;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.integerValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.logicalValue;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.singleElement;
import static com.oracle.truffle.r.nodes.builtin.CastBuilder.Predef.stringValue;
import static com.oracle.truffle.r.runtime.RVisibility.OFF;
import static com.oracle.truffle.r.runtime.builtins.RBehavior.COMPLEX;
import static com.oracle.truffle.r.runtime.builtins.RBuiltinKind.PRIMITIVE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.runtime.RArguments;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.RError.Message;
import com.oracle.truffle.r.runtime.RRuntime;
import com.oracle.truffle.r.runtime.builtins.RBuiltin;
import com.oracle.truffle.r.runtime.conn.StdConnections;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.env.frame.FrameSlotChangeMonitor;

/**
 *
 * Inspects metadata of a function frame or a frame associated with an environment. If {@code env}
 * parameter is given, then the frame associated with that environment is used for the inspection,
 * otherwise inspects a frame of an enclosing environment given by {@code enclosingFrameNum}.
 *
 * Inspects metadata of frame given by {@code enclosingFrameNum} parameter. It works similarly to
 * `which` parameter in {@code parent.env}, i.e., 0 corresponds to the current frame (the frame of
 * the function that calls {@code .fastr.inspect.frame}, 1 corresponds to the enclosing environment
 * of the current function, etc. If, during the iteration of enclosing frames, we bump into global
 * environment, an error is reported.
 *
 * {@code identifiers} is a character vector of all the identifiers that should be inspected in the
 * frame. The inspection looks for a particular {@code FrameSlotInfo} and shows it. If the
 * identifier does not exist in the frame, warning is printed and it is ignored. If
 * {@code identifiers} is missing or NULL, all the identifiers in the frame are inspected.
 *
 * If {@code verbose} is TRUE, then also FrameDescriptor metadata is printed, along with all the
 * FrameSlotInfos for given identifiers.
 *
 * The output of this function is logged to the standard output.
 */
@RBuiltin(name = ".fastr.inspect.frame", visibility = OFF, kind = PRIMITIVE, parameterNames = {"enclosingFrameNum", "identifiers", "env", "verbose"}, behavior = COMPLEX)
public abstract class FastRInspectFrame extends RBuiltinNode.Arg4 {
    static {
        Casts casts = new Casts(FastRInspectFrame.class);
        casts.arg("enclosingFrameNum").mapMissing(constant(1)).mustBe(integerValue()).asIntegerVector().mustBe(singleElement()).findFirst();
        casts.arg("identifiers").mapMissing(emptyStringVector()).mapNull(emptyStringVector()).mustBe(stringValue()).asStringVector();
        casts.arg("env").allowMissing().mustBe(instanceOf(REnvironment.class));
        casts.arg("verbose").mapMissing(constant(RRuntime.LOGICAL_FALSE)).mustBe(logicalValue()).asLogicalVector().findFirst();
    }

    @Specialization
    public Object inspectFrameOfEnclosingEnvironment(VirtualFrame currentFrame, int enclosingFrameNum, RStringVector identifiersVector, @SuppressWarnings("unused") RMissing env, byte verbose) {
        CompilerAsserts.neverPartOfCompilation();
        MaterializedFrame enclosingFrame = currentFrame.materialize();
        for (int i = 1; i < enclosingFrameNum; i++) {
            enclosingFrame = RArguments.getEnclosingFrame(enclosingFrame);
            if (enclosingFrame == null) {
                throw error(Message.GENERIC, "While iterating enclosing frames, we run into global frame, run the function " +
                                "again, but with different `enclosingFrameNum` argument");
            }
        }
        assert enclosingFrame != null;
        inspectFrame(enclosingFrame, filterIdentifiersInFrame(identifiersVector, enclosingFrame), RRuntime.fromLogical(verbose));
        return RNull.instance;
    }

    @Specialization
    public Object inspectFrameOfEnvironment(@SuppressWarnings("unused") int enclosingFrameNum, RStringVector identifiersVector, REnvironment env, byte verbose) {
        CompilerAsserts.neverPartOfCompilation();
        MaterializedFrame envFrame = env.getFrame();
        List<Object> identifiersToInspect = filterIdentifiersInFrame(identifiersVector, envFrame);
        inspectFrame(envFrame, identifiersToInspect, RRuntime.fromLogical(verbose));
        return RNull.instance;
    }

    private static List<Object> filterIdentifiersInFrame(RStringVector identifiersVector, MaterializedFrame frame) {
        String[] identifiers = identifiersVector.getReadonlyStringData();
        List<Object> identifiersToInspect;
        if (identifiers.length == 0) {
            identifiersToInspect = getAllIdentifiersInFrame(frame);
        } else {
            identifiersToInspect = new ArrayList<>();
            for (String identifier : identifiers) {
                if (isIdentifierInFrame(frame, identifier)) {
                    identifiersToInspect.add(identifier);
                } else {
                    RError.warning(RError.NO_CALLER, Message.GENERIC, String.format("Identifier '%s' is ignored, because it is not present in the enclosing frame", identifier));
                }
            }
        }
        return identifiersToInspect;
    }

    private static void inspectFrame(MaterializedFrame frame, List<Object> identifiers, boolean verbose) {
        if (verbose) {
            writeLine("Frame Descriptor metadata: ");
            writeLine(FrameSlotChangeMonitor.frameDescriptorMetadataToString(frame.getFrameDescriptor()));
            writeLine("");
            writeLine("frameDescriptor.toString(): ");
            writeLine(frame.getFrameDescriptor().toString());
            writeLine("");
        }
        writeLine("Slot info of identifiers: ");
        for (Object identifier : identifiers) {
            assert isIdentifierInFrame(frame, identifier);
            String frameSlotInfo = FrameSlotChangeMonitor.frameSlotInfoToString(frame, identifier);
            writeLine("    " + frameSlotInfo);
        }
    }

    private static boolean isIdentifierInFrame(Frame frame, Object identifier) {
        return FrameSlotChangeMonitor.containsIdentifier(frame.getFrameDescriptor(), identifier);
    }

    private static List<Object> getAllIdentifiersInFrame(Frame frame) {
        return new ArrayList<>(FrameSlotChangeMonitor.getIdentifiers(frame.getFrameDescriptor()));
    }

    @TruffleBoundary
    private static void writeLine(String msg) {
        try {
            StdConnections.getStdout().writeString(msg, true);
        } catch (IOException ex) {
            throw RError.error(RError.NO_CALLER, RError.Message.GENERIC, ex.getMessage() == null ? ex : ex.getMessage());
        }
    }
}
