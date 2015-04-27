package com.oracle.truffle.r.nodes.test;

import org.junit.*;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.r.engine.*;
import com.oracle.truffle.r.options.*;
import com.oracle.truffle.r.runtime.*;
import com.oracle.truffle.r.runtime.data.*;
import com.oracle.truffle.r.runtime.ffi.*;

public class TestBase {

    @BeforeClass
    public static void setupClass() {
        Load_RFFIFactory.initialize();
        FastROptions.initialize();
        REnvVars.initialize();
        ROptions.initialize();
        ROptions.setValue("defaultPackages", RDataFactory.createStringVector(new String[]{}, true));
        REngine.initialize(new String[0], new ConsoleHandler(), false, true);
    }

    private static class ConsoleHandler implements RContext.ConsoleHandler {
        private final StringBuilder buffer = new StringBuilder();

        @TruffleBoundary
        public void println(String s) {
            buffer.append(s);
            buffer.append('\n');
        }

        @TruffleBoundary
        public void print(String s) {
            buffer.append(s);
        }

        @TruffleBoundary
        public void printf(String format, Object... args) {
            buffer.append(String.format(format, args));
        }

        public String readLine() {
            return null;
        }

        public boolean isInteractive() {
            return false;
        }

        @TruffleBoundary
        public void printErrorln(String s) {
            println(s);
        }

        @TruffleBoundary
        public void printError(String s) {
            print(s);
        }

        public void redirectError() {
            // always
        }

        public String getPrompt() {
            return null;
        }

        public void setPrompt(String prompt) {
            // ignore
        }

        @TruffleBoundary
        void reset() {
            buffer.delete(0, buffer.length());
        }

        public int getWidth() {
            return RContext.CONSOLE_WIDTH;
        }

    }

}
