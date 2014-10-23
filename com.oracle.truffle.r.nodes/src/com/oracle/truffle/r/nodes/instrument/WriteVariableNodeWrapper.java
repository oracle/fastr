/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oracle.truffle.r.nodes.instrument;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrument.Probe;
import com.oracle.truffle.api.instrument.ProbeNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.r.nodes.RNode;
import com.oracle.truffle.r.nodes.access.WriteVariableNode;
import com.oracle.truffle.r.runtime.RDeparse;

public class WriteVariableNodeWrapper extends WriteVariableNode implements ProbeNode.WrapperNode {
    @Node.Child WriteVariableNode child;
    @Node.Child private ProbeNode probeNode;

    public WriteVariableNodeWrapper(WriteVariableNode child) {
        assert child != null;
        assert !(child instanceof WriteVariableNodeWrapper);
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
    public final void execute(VirtualFrame frame, Object value) {
        probeNode.enter(child, frame);

        try {
            child.execute(frame, value);
        } catch (Exception e) {
            probeNode.returnExceptional(child, frame, e);
            throw (e);
        }
    }

    @Override
    public void deparse(RDeparse.State state) {
        child.deparse(state);
    }

    public boolean isArgWrite() {
        return child.isArgWrite();
    }

    public RNode getRhs() {
        return child.getRhs();
    }
    
    public String getName() {
        return child.getName();
    }

    @Override
    public boolean isInstrumentable() {
        return false;
    }

    @Override
    public boolean isSyntax() {
        return false;
    }
}
