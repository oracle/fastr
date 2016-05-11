package com.oracle.truffle.r.nodes.builtin;

import static com.oracle.truffle.r.nodes.unary.CastNode.Not.negateType;
import static com.oracle.truffle.r.nodes.unary.CastNode.TypeExpr.atom;
import static com.oracle.truffle.r.nodes.unary.CastNode.TypeExpr.union;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.oracle.truffle.r.nodes.unary.CastNode.TypeConjunction;
import com.oracle.truffle.r.runtime.data.RIntSequence;
import com.oracle.truffle.r.runtime.data.RNull;
import com.oracle.truffle.r.runtime.data.RSequence;
import com.oracle.truffle.r.runtime.data.model.RAbstractIntVector;
import com.oracle.truffle.r.runtime.data.model.RAbstractStringVector;

public class TypeExprTest {

    @Test
    public void testNormalize() {
        Assert.assertEquals(toSet(String.class), union(String.class).normalize());
        Assert.assertEquals(toSet(String.class, Integer.class), union(String.class, Integer.class).normalize());
        Assert.assertEquals(toSet(RIntSequence.class), atom(RIntSequence.class).and(atom(RSequence.class)).normalize());
        Assert.assertEquals(toSet(String.class), atom(String.class).and(atom(RNull.class).not()).normalize());
        Assert.assertEquals(toSet(TypeConjunction.create(negateType(String.class), negateType(Integer.class))),
                        atom(String.class).not().and(atom(Integer.class).not()).normalize());
        Assert.assertEquals(toSet(), atom(String.class).not().and(atom(String.class)).normalize());
        Assert.assertEquals(toSet(TypeConjunction.create(RAbstractIntVector.class, RAbstractStringVector.class)), atom(RAbstractIntVector.class).and(atom(RAbstractStringVector.class)).normalize());
    }

    private static Set<Type> toSet(Type... classes) {
        return new HashSet<>(Arrays.asList(classes));
    }

}
