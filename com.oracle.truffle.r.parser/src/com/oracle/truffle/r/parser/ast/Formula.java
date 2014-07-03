package com.oracle.truffle.r.parser.ast;

import java.util.*;

import com.oracle.truffle.api.source.*;

public class Formula extends ASTNode {

    private final ASTNode response;
    private final ASTNode model;

    public Formula(SourceSection source, ASTNode response, ASTNode model) {
        this.response = response;
        this.model = model;
        setSource(source);
    }

    public static Formula create(SourceSection source, ASTNode response, ASTNode model) {
        return new Formula(source, response, model);
    }

    public ASTNode getResponse() {
        return response;
    }

    public ASTNode getModel() {
        return model;
    }

    @Override
    public <R> R accept(Visitor<R> v) {
        return v.visit(this);
    }

    @Override
    public <R> List<R> visitAll(Visitor<R> v) {
        throw new IllegalStateException("should not reach here");
    }

}
