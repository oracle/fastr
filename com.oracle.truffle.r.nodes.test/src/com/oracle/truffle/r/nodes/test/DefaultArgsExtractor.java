/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.nodes.test;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.api.vm.PolyglotEngine.Value;
import com.oracle.truffle.r.nodes.casts.Samples;
import com.oracle.truffle.r.runtime.RDeparse;
import com.oracle.truffle.r.runtime.RSource;
import com.oracle.truffle.r.runtime.data.RLanguage;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RPairList;
import com.oracle.truffle.r.runtime.data.RStringVector;
import com.oracle.truffle.r.runtime.data.RSymbol;
import com.oracle.truffle.r.runtime.data.model.RAbstractContainer;
import com.oracle.truffle.r.test.generate.FastRSession;

/**
 * This helper class extracts default argument values from an R function.
 */
class DefaultArgsExtractor {

    private final FastRSession fastRSession;

    DefaultArgsExtractor(FastRSession fastRSession) {
        this.fastRSession = fastRSession;
    }

    Map<String, Samples<?>> extractDefaultArgs(String functionName) {
        final PolyglotEngine vm = fastRSession.createTestContext(null);

        try {
            HashMap<String, Samples<?>> samplesMap = new HashMap<>();

            Source source = RSource.fromTextInternal("formals(" + functionName + ")",
                            RSource.Internal.UNIT_TEST);
            Value defArgVal = vm.eval(source);
            if (defArgVal.get() instanceof RPairList) {
                RPairList formals = (RPairList) defArgVal.get();
                RStringVector names = formals.getNames(null);

                for (int i = 0; i < names.getLength(); i++) {
                    String name = names.getDataAt(i);
                    Object defVal = formals.getDataAtAsObject(i);

                    if (defVal instanceof RLanguage) {
                        String deparsedDefVal = RDeparse.deparse(defVal);
                        Value eval = vm.eval(RSource.fromTextInternal(deparsedDefVal,
                                        RSource.Internal.UNIT_TEST));
                        defVal = eval.get();

                        if (defVal == null) {
                            samplesMap.put(name, Samples.singleton(RNull.instance));
                        } else if (defVal instanceof RAbstractContainer) {
                            // enumerated default arg values
                            RAbstractContainer enumArgs = (RAbstractContainer) defVal;
                            HashSet<Object> enumPosSamples = new HashSet<>();
                            for (int j = 0; j < enumArgs.getLength(); j++) {
                                // enumPosSamples.add(enumArgs.getDataAtAsObject(j));
                                // It is assumed that the corresponding builtin wrapper function
                                // calls 'match.arg'
                                enumPosSamples.add(j);
                            }
                            samplesMap.put(name, new Samples<>(name, enumPosSamples, Collections.emptySet(), x -> true));
                        } else {
                            samplesMap.put(name, Samples.singleton(defVal));
                        }

                    } else if (defVal instanceof RSymbol) {
                        continue;
                    } else {
                        samplesMap.put(name, Samples.singleton(defVal));
                    }
                }

            }

            return samplesMap;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            vm.dispose();
        }
    }
}
