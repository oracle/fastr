/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.oracle.truffle.r.nodes.instrument;

import com.oracle.truffle.api.instrument.SyntaxTag;

/**
 * R-specific tags.
 */
public enum RSyntaxTag implements SyntaxTag {
    DEBUGGED("debug set", "debug invoked on a function"),
    FUNCTION_BODY("function body", "a function body");

    private final String name;
    private final String description;

    private RSyntaxTag(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

}
