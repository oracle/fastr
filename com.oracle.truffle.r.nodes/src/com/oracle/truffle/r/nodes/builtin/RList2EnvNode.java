package com.oracle.truffle.r.nodes.builtin;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.r.runtime.RError;
import com.oracle.truffle.r.runtime.data.RList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.env.REnvironment;
import com.oracle.truffle.r.runtime.nodes.RBaseNode;

/**
 * Abstracted for use by {@code List2Env}, {@code AsEnvironment}, {@code SubsituteDirect}.
 */
public abstract class RList2EnvNode extends RBaseNode {

    public abstract REnvironment execute(Object list, Object env);

    @Specialization
    protected REnvironment doList2Env(RList list, REnvironment env) {
        RStringVector names = list.getNames();
        if (names == null) {
            throw RError.error(this, RError.Message.LIST_NAMES_SAME_LENGTH);
        }
        for (int i = list.getLength() - 1; i >= 0; i--) {
            String name = names.getDataAt(i);
            if (name.length() == 0) {
                throw RError.error(this, RError.Message.ZERO_LENGTH_VARIABLE);
            }
            // in case of duplicates, last element in list wins
            if (env.get(name) == null) {
                env.safePut(name, list.getDataAt(i));
            }
        }
        return env;
    }

    @SuppressWarnings("unused")
    @Fallback
    protected REnvironment doList2Env(Object obj, REnvironment env) {
        throw RError.error(this, RError.Message.FIRST_ARGUMENT_NOT_NAMED_LIST);
    }

}
