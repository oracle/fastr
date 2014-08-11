package com.oracle.truffle.r.nodes.builtin.fastr;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.graal.debug.*;
import com.oracle.graal.debug.Debug.Scope;
import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.api.nodes.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.access.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;

/**
 * Run a FastR function, and dump its AST to IGV before and after running. If no function is passed,
 * this builtin does not do anything.
 */
@RBuiltin(name = "fastr.rundump", parameterNames = {"function"}, kind = PRIMITIVE)
public abstract class FastRRunDump extends RInvisibleBuiltinNode {

    // TODO Make this more versatile by allowing actual function calls with arguments to be
    // observed. This requires ... to work properly.

    @Child private IndirectCallNode call = Truffle.getRuntime().createIndirectCallNode();

    private final GraphPrintVisitor graphPrinter = new GraphPrintVisitor();

    @Override
    public RNode[] getParameterValues() {
        return new RNode[]{ConstantNode.create(RNull.instance)};
    }

    @Specialization
    public Object runDump(RNull function) {
        return function;
    }

    @Specialization
    public Object runDump(VirtualFrame frame, RFunction function) {
        controlVisibility();
        Object r = RNull.instance;
        graphPrinter.beginGroup(RRuntime.toString(function));
        try (Scope s = Debug.scope("FastR")) {
            graphPrinter.beginGraph("before").visit(function.getTarget().getRootNode());
            r = call.call(frame, function.getTarget(), RArguments.create(function));
            graphPrinter.beginGraph("after").visit(function.getTarget().getRootNode());
        } catch (Throwable t) {
            Debug.handle(t);
        } finally {
            graphPrinter.printToNetwork(true);
        }
        return r;
    }

}
