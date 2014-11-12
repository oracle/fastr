package com.oracle.truffle.r.nodes.function.opt;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.function.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.RPromise.RPromiseFactory;

/**
 * A optimizing {@link PromiseNode}: It evaluates a constant directly.
 */
public final class OptConstantPromiseNode extends PromiseNode {

    @Child private RNode constantExpr;
    @CompilationFinal private boolean isEvaluated = false;
    @CompilationFinal private Object constant = null;

    public OptConstantPromiseNode(RPromiseFactory factory) {
        super(factory);
        this.constantExpr = (RNode) factory.getExpr();
    }

    /**
     * Creates a new {@link RPromise} every time.
     */
    @Override
    public Object execute(VirtualFrame frame) {
        if (!isEvaluated) {
            // Eval constant on first time and make it compile time constant afterwards
            CompilerDirectives.transferToInterpreterAndInvalidate();
            constant = constantExpr.execute(frame);
            isEvaluated = true;
        }

        return factory.createArgEvaluated(constant);
    }
}