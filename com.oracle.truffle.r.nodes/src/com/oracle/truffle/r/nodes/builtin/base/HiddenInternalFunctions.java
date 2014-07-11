/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2012, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2013, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.nodes.builtin.base;

import static com.oracle.truffle.r.runtime.RBuiltinKind.*;

import com.oracle.truffle.api.*;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.nodes.*;
import com.oracle.truffle.r.nodes.builtin.*;
import com.oracle.truffle.r.nodes.builtin.base.EvalFunctions.Eval;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.REnvironment.PutException;
import com.oracle.truffle.r.runtime.RError.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.data.model.*;

/**
 * Private, undocumented, {@code .Internal} functions transcribed from GnuR.
 */
public class HiddenInternalFunctions {

    /**
     * Transcribed from GnuR {@code do_makeLazy} in src/main/builtin.c.
     */
    @RBuiltin(name = "makeLazy", kind = RBuiltinKind.INTERNAL)
    public abstract static class MakeLazy extends RBuiltinNode {
        @Child Eval eval;

        private void initEval() {
            if (eval == null) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                eval = insert(EvalFunctionsFactory.EvalFactory.create(new RNode[3], this.getBuiltin()));
            }
        }

        @Specialization
        public RNull doMakeLazy(@SuppressWarnings("unused") VirtualFrame frame, RAbstractStringVector names, RList values, RLanguage expr, REnvironment eenv, REnvironment aenv) {
            controlVisibility();
            initEval();
            for (int i = 0; i < names.getLength(); i++) {
                String name = names.getDataAt(i);
                @SuppressWarnings("unused")
                RIntVector intVec = (RIntVector) values.getDataAt(i);
                // Short cut since intVec evaluates to itself

                // then what to do with it?

                try {
                    aenv.put(name, RDataFactory.createPromise(expr, eenv));
                } catch (PutException ex) {
                    throw RError.error(getEncapsulatingSourceSection(), ex);
                }
            }
            return RNull.instance;
        }
    }

    /**
     * Transcribed from {@code do_importIntoEnv} in src/main/envir.c.
     *
     * This function copies values of variables from one environment to another environment,
     * possibly with different names. Promises are not forced and active bindings are preserved.
     */
    @RBuiltin(name = "importIntoEnv", kind = INTERNAL)
    public abstract static class ImportIntoEnv extends RBuiltinNode {
        @Specialization
        public RNull importIntoEnv(REnvironment impEnv, RAbstractStringVector impNames, REnvironment expEnv, RAbstractStringVector expNames) {
            controlVisibility();
            int length = impNames.getLength();
            if (length != expNames.getLength()) {
                throw RError.error(getEncapsulatingSourceSection(), Message.IMP_EXP_NAMES_MATCH);
            }
            for (int i = 0; i < length; i++) {
                String impsym = impNames.getDataAt(i);
                String expsym = expNames.getDataAt(i);
                Object binding = null;
                // TODO name translation, and a bunch of other special cases
                for (REnvironment env = expEnv; env != REnvironment.emptyEnv() && binding == null; env = env.getParent()) {
                    if (env == REnvironment.baseNamespaceEnv()) {
                        assert false;
                    } else {
                        binding = env.get(expsym);
                    }
                }
                try {
                    impEnv.put(impsym, binding);
                } catch (PutException ex) {
                    throw RError.error(getEncapsulatingSourceSection(), ex);
                }

            }
            return RNull.instance;
        }
    }

}
