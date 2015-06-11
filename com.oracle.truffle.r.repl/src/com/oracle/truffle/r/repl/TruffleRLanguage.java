package com.oracle.truffle.r.repl;

import java.io.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.debug.*;
import com.oracle.truffle.api.instrument.*;
import com.oracle.truffle.api.source.*;
import com.oracle.truffle.r.repl.debug.*;
import com.oracle.truffle.r.runtime.*;

@TruffleLanguage.Registration(name = "R", version = "3.1.3", mimeType = "application/x-r")
public class TruffleRLanguage extends TruffleLanguage {

    private DebugSupportProvider debugSupport;

    public TruffleRLanguage(Env env) {
        super(env);
    }

    @Override
    protected Object eval(Source code) throws IOException {
        throw RInternalError.unimplemented();
    }

    @Override
    protected Object findExportedSymbol(String globalName, boolean onlyExplicit) {
        throw RInternalError.unimplemented();
    }

    @Override
    protected Object getLanguageGlobal() {
        throw RInternalError.unimplemented();
    }

    @Override
    protected boolean isObjectOfLanguage(Object object) {
        return false;
    }

    @Override
    protected ToolSupportProvider getToolSupport() {
        return getDebugSupport();
    }

    @Override
    protected DebugSupportProvider getDebugSupport() {
        if (debugSupport == null) {
            debugSupport = new RDebugSupportProvider();
        }
        return debugSupport;
    }

}
