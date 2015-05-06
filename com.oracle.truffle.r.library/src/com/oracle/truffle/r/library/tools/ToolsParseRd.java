package com.oracle.truffle.r.library.tools;

import com.oracle.truffle.r.runtime.conn.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;
import com.oracle.truffle.r.runtime.env.*;

public class ToolsParseRd {
    public static Object parseRd(RConnection con, REnvironment srcfile, String encoding, boolean verbose, RAbstractStringVector basename, boolean fragment, boolean warningCalls) {
        return RNull.instance;
    }
}
