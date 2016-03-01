package com.oracle.truffle.r.nodes.builtin.base.printer;

import static org.junit.Assert.*;
import org.junit.Test;

public class UtilsTest {

    @Test
    public void testIsValidName() {
        assertFalse(Utils.isValidName(""));
        assertFalse(Utils.isValidName("7"));
        assertTrue(Utils.isValidName(".7"));
        assertTrue(Utils.isValidName("x7_y.z"));
        assertFalse(Utils.isValidName("x%"));
        assertTrue(Utils.isValidName("..."));
        assertFalse(Utils.isValidName("while"));
    }

}
