package com.oracle.truffle.r.runtime.nodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.r.runtime.RInternalError;

/**
 * A subclass of {@link RNode} that carries {@link SourceSection}. Wherever possible, a node that
 * implements {@link RSyntaxNode} should subclass this class.
 */
public abstract class RSourceSectionNode extends RNode {
    @CompilationFinal private SourceSection sourceSectionR; // temp disambiguate for debugging

    protected RSourceSectionNode(SourceSection sourceSection) {
        this.sourceSectionR = sourceSection;
    }

    public void setSourceSection(SourceSection sourceSection) {
        this.sourceSectionR = sourceSection;
    }

    @Override
    public SourceSection getSourceSection() {
        return sourceSectionR;
    }

    @Override
    public void clearSourceSection() {
        sourceSectionR = null;
    }

    @Override
    public void assignSourceSection(SourceSection sourceSection) {
        // catch code that needs attention
        throw RInternalError.shouldNotReachHere("assignSourceSection");
    }

}
