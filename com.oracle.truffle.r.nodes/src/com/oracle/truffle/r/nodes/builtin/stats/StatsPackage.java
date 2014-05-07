package com.oracle.truffle.r.nodes.builtin.stats;

import com.oracle.truffle.r.nodes.builtin.*;

public class StatsPackage extends RBuiltinPackage {

    public StatsPackage() {
        loadBuiltins();
    }

    @Override
    public String getName() {
        return "stats";
    }

}
