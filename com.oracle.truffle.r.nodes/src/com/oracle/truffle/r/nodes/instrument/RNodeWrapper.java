package com.oracle.truffle.r.nodes.instrument;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.instrument.*;
import com.oracle.truffle.api.instrument.ProbeNode.WrapperNode;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.runtime.RDeparse.State;
import com.oracle.truffle.r.runtime.env.REnvironment;

public final class RNodeWrapper extends RNode implements WrapperNode {

    @Child private RNode child;
    @Child private ProbeNode probeNode;

    public RNodeWrapper(RNode child) {
        assert child != null;
        assert !(child instanceof RNodeWrapper);
        this.child = child;
    }

    public String instrumentationInfo() {
        return "Wrapper node for FastR";
    }

    public Node getChild() {
        return child;
    }

    public Probe getProbe() {
        try {
            return probeNode.getProbe();
        } catch (IllegalStateException e) {
            throw new IllegalStateException("A lite-Probed wrapper has no explicit Probe");
        }
    }

    public void insertProbe(ProbeNode newProbeNode) {
        this.probeNode = newProbeNode;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        probeNode.enter(child, frame);

        Object result;

        try {
            result = child.execute(frame);
            probeNode.returnValue(child, frame, result);
        } catch (Exception e) {
            probeNode.returnExceptional(child, frame, e);
            throw (e);
        }

        return result;
    }

    @Override
    public void deparse(State state) {
        child.deparse(state);
    }

    @Override
    public boolean isInstrumentable() {
        return false;
    }

    @Override
    public boolean isSyntax() {
        return false;
    }

    @Override
    public RNode substitute(REnvironment env) {
        // TODO tagging preservation?
        RNodeWrapper wrapperSub =  new RNodeWrapper(child.substitute(env));
        ProbeNode.insertProbe(wrapperSub);
        return wrapperSub;
    }

}
