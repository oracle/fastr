/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oracle.truffle.r.nodes.builtin.fastr;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.instrument.Probe;
import com.oracle.truffle.r.nodes.RNode;
import com.oracle.truffle.r.nodes.access.ConstantNode;
import com.oracle.truffle.r.nodes.builtin.RBuiltinNode;
import com.oracle.truffle.r.nodes.builtin.RInvisibleBuiltinNode;
import com.oracle.truffle.r.nodes.function.FunctionDefinitionNode;
import com.oracle.truffle.r.nodes.instrument.REntryCounters;
import com.oracle.truffle.r.nodes.instrument.RInstrument;
import com.oracle.truffle.r.nodes.instrument.RSyntaxTag;
import com.oracle.truffle.r.runtime.RBuiltin;
import static com.oracle.truffle.r.runtime.RBuiltinKind.PRIMITIVE;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RFunction;
import com.oracle.truffle.r.runtime.data.RMissing;
import com.oracle.truffle.r.runtime.data.RNull;
import java.util.UUID;

/**
 *
 * @author mjj
 */
public class FastRCallCounting {

    @RBuiltin(name = "fastr.createcc", kind = PRIMITIVE, parameterNames = {"func"})
    public abstract static class FastRCreateCallCounter extends RInvisibleBuiltinNode {

        @Specialization
        protected RNull createCallCounter(@SuppressWarnings("unused") RMissing function) {
            controlVisibility();
            throw RError.error(RError.Message.ARGUMENTS_PASSED_0_1);
        }

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance)};
        }

        @Specialization
        protected Object createCallCounter(RFunction function) {
            controlVisibility();
            if (!function.isBuiltin()) {
                FunctionDefinitionNode fdn = (FunctionDefinitionNode) function.getRootNode();
                UUID uuid = fdn.getUUID();
                if (REntryCounters.findCounter(uuid) == null) {
                    Probe probe = RInstrument.findSingleProbe(uuid, RSyntaxTag.FUNCTION_BODY);
                    if (probe == null) {
                        throw RError.error(getEncapsulatingSourceSection(), RError.Message.GENERIC, "failed to apply counter");
                    } else {
                        REntryCounters.Function counter = new REntryCounters.Function(uuid);
                        probe.attach(counter.instrument);
                    }
                }
            }
            return RNull.instance;
        }
    }

    @RBuiltin(name = "fastr.getcc", kind = PRIMITIVE, parameterNames = {"func"})
    public abstract static class FastRGetCallCount extends RBuiltinNode {

        @Specialization
        protected RNull getCallCount(@SuppressWarnings("unused") RMissing function) {
            controlVisibility();
            throw RError.error(RError.Message.ARGUMENTS_PASSED_0_1);
        }

        @Override
        public RNode[] getParameterValues() {
            return new RNode[]{ConstantNode.create(RMissing.instance)};
        }

        @Specialization
        protected Object getCallCount(RFunction function) {
            controlVisibility();
            if (!function.isBuiltin()) {
                FunctionDefinitionNode fdn = (FunctionDefinitionNode) function.getRootNode();
                REntryCounters.Function counter = (REntryCounters.Function) REntryCounters.findCounter(fdn.getUUID());
                if (counter == null) {
                    throw RError.error(getEncapsulatingSourceSection(), RError.Message.GENERIC, "no associated counter");
                }
                return counter.getEnterCount();
            }
            return RNull.instance;
        }
    }

}
