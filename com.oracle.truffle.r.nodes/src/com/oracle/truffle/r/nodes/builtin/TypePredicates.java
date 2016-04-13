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
package com.oracle.truffle.r.nodes.builtin;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public final class TypePredicates {

    public static <T> T is(boolean b) {
        if (b) {
            return null;
        } else {
            throw InvalidType.instance;
        }
    }

    public interface ReflectiveFunction<T, R> extends ReflectiveLambda<R>, Function<T, R> {
    }

    public interface ReflectivePredicate<R> extends ReflectiveFunction<Object, R> {

        default Set<Class<?>> returnTypes() {
            return Collections.singleton(returnType());
        }

        default Predicate<Object> predicate() {
            return x -> test(x);
        }

        default boolean test(Object x) {
            try {
                apply(x);
                return true;
            } catch (InvalidType it) {
                return false;
            }
        }

        default <S> ReflectivePredicate<S> or(ReflectivePredicate<?> p2) {
            @SuppressWarnings("serial")
            class UnionTypePredicate implements ReflectivePredicate<S> {

                public Set<Class<?>> returnTypes() {
                    HashSet<Class<?>> newRetTypes = new HashSet<>(ReflectivePredicate.this.returnTypes());
                    newRetTypes.addAll(p2.returnTypes());
                    return newRetTypes;
                }

                public S apply(Object t) {
                    try {
                        ReflectivePredicate.this.apply(t);
                    } catch (InvalidType it) {
                        p2.apply(t);
                    }
                    return null;
                }
            }
            return new UnionTypePredicate();
        }
    }

    @SuppressWarnings("serial")
    private static class InvalidType extends RuntimeException {

        static final InvalidType instance = new InvalidType();

        private InvalidType() {

        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }

    }

    public interface ReflectiveLambda<T> extends Serializable {

        default Method lambdaMethod() {
            Class<?> containingClass;
            SerializedLambda serialLambda;
            try {
                Method writeReplaceMethod = getClass().getDeclaredMethod("writeReplace");
                writeReplaceMethod.setAccessible(true);
                serialLambda = (SerializedLambda) writeReplaceMethod.invoke(this);
                String containingClassName = serialLambda.getImplClass().replaceAll("/", ".");
                containingClass = Class.forName(containingClassName);
            } catch (Exception e) {
                throw new RuntimeException("Unable to resolve the containing class of the lambda", e);
            }

            return Arrays.asList(containingClass.getDeclaredMethods())
                            .stream()
                            .filter(method -> Objects.equals(method.getName(), serialLambda.getImplMethodName()))
                            .findFirst()
                            .orElseThrow(UnableToResolveLambdaMethodException::new);

        }

        @SuppressWarnings("unchecked")
        default Class<T> returnType() {
            return (Class<T>) lambdaMethod().getReturnType();
        }

    }

    @SuppressWarnings("serial")
    public static final class UnableToResolveLambdaMethodException extends RuntimeException {
        private UnableToResolveLambdaMethodException() {
            super("Unable to resolve the lambda method");
        }
    }
}
