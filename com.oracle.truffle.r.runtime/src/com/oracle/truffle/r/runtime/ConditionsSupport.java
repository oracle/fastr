/*
 * This material is distributed under the GNU General Public License
 * Version 2. You may review the terms of this license at
 * http://www.gnu.org/licenses/gpl-2.0.html
 *
 * Copyright (c) 1995-2015, The R Core Team
 * Copyright (c) 2003, The R Foundation
 * Copyright (c) 2015, Oracle and/or its affiliates
 *
 * All rights reserved.
 */
package com.oracle.truffle.r.runtime;

import com.oracle.truffle.api.frame.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.env.*;

/**
 * Condition handling. Derived from GnUR src/main/errors.c
 */
public class ConditionsSupport {
    /*
     * These values are either NULL or an RPairList.
     */
    private static Object restartStack = RNull.instance;
    private static Object handlerStack = RNull.instance;
    private static final Object RESTART_TOKEN = new Object();

    public static Object getHandlerStack() {
        return handlerStack;
    }

    public static Object getRestartStack() {
        return restartStack;
    }

    public static void restoreStacks(Object savedHandlerStack, Object savedRestartStack) {
        handlerStack = savedHandlerStack;
        restartStack = savedRestartStack;
    }

    public static Object createHandlers(RStringVector classes, RList handlers, REnvironment parentEnv, Object target, byte calling) {
        Object oldStack = handlerStack;
        Object newStack = oldStack;
        RList result = RDataFactory.createList(new Object[]{RNull.instance, RNull.instance, RNull.instance});
        int n = handlers.getLength();
        for (int i = n - 1; i >= 0; i--) {
            String klass = classes.getDataAt(i);
            Object handler = handlers.getDataAt(i);
            RList entry = mkHandlerEntry(klass, parentEnv, handler, target, result, calling);
            newStack = RDataFactory.createPairList(entry, newStack);
        }
        handlerStack = newStack;
        return oldStack;
    }

    private static RList mkHandlerEntry(String klass, REnvironment parentEnv, Object handler, Object rho, RList result, @SuppressWarnings("unused") byte calling) {
        Object[] data = new Object[5];
        data[0] = klass;
        data[1] = parentEnv;
        data[2] = handler;
        data[3] = rho;
        data[4] = result;
        // SETLEVELS; GnuR stores this in the "general purpose slot that we don't have (yet)
        return RDataFactory.createList(data);
    }

    public static void addRestart(RList restart) {
        restartStack = RDataFactory.createPairList(restart, restartStack, RNull.instance);
    }

    public static void signalError(MaterializedFrame frame, String errorMsg) {
        Object oldStack = handlerStack;
        RPairList pList;
        while ((pList = findSimpleErrorHandler()) != null) {
            RList entry = (RList) pList.car();
            handlerStack = pList.cdr();
            // Checks the calling fields that we are not storing
            if (true) {
                if (entry.getDataAt(2) == RESTART_TOKEN) {
                    return;
                } else {
                    RFunction handler = (RFunction) entry.getDataAt(2);
                    RStringVector errorMsgVec = RDataFactory.createStringVectorFromScalar(errorMsg);
                    RContext.getRASTHelper().handleSimpleError(handler, errorMsgVec, RNull.instance, RArguments.getDepth(frame));
                }
            }
        }
        handlerStack = oldStack;
    }

    private static RPairList findSimpleErrorHandler() {
        Object list = handlerStack;
        while (list != RNull.instance) {
            RPairList pList = (RPairList) list;
            RList entry = (RList) pList.car();
            String klass = (String) entry.getDataAt(0);
            if (klass.equals("simpleError") || klass.equals("error") || klass.equals("condition")) {
                return pList;
            }
            list = pList.cdr();
        }
        return null;
    }

}
