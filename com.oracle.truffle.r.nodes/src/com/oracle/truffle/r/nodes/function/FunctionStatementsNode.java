/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oracle.truffle.r.nodes.function;

import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.nodes.RNode;
import com.oracle.truffle.r.nodes.control.SequenceNode;

/**
 * Encapsulates the sequence of statements (expressions) of a function. Has no
 * specific execute behavior but is an important placeholder for debugging
 * instrumentation.
 */
public class FunctionStatementsNode extends SequenceNode {

    public FunctionStatementsNode(RNode[] sequence) {
        super(sequence);
    }

    public FunctionStatementsNode(SourceSection src, RNode[] sequence) {
        super(src, sequence);
    }
    
    public FunctionStatementsNode(RNode node) {
        super(node);
    }
    
    @Override
    public boolean isInstrumentable() {
        return true;
    }
    
}
