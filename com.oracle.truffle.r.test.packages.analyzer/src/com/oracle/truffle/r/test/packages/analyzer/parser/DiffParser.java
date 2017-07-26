/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
package com.oracle.truffle.r.test.packages.analyzer.parser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.oracle.truffle.r.test.packages.analyzer.Location;

public class DiffParser {

    private final LogFileParser parent;

    /**
     * Diff change command pattern.<br>
     * Format (EBNF): diffCmd = int [ ',' int ] ( 'a' | 'c' | 'd' ) int [ ',' int ].<br>
     * <p>
     * Capture Groups:<br>
     * group(1) = left from line<br>
     * group(3) = left to line<br>
     * group(5) = right from line<br>
     * group(7) = right to line<br>
     * group("CMD") = command<br>
     * </p>
     */
    static final Pattern CHANGE_CMD_PATTERN = Pattern.compile("(\\d+)(,(\\d+))?(?<CMD>a|c|d)(\\d+)(,(\\d+))?");

    protected DiffParser(LogFileParser parent) {
        this.parent = parent;
    }

    private Location loc(int l) {
        return new Location(parent.getLogFile().getPath(), l);
    }

    public List<DiffParser.DiffChunk> parseDiff() throws IOException {
        List<DiffParser.DiffChunk> diffResult = new LinkedList<>();

        for (;;) {
            Matcher matcher = CHANGE_CMD_PATTERN.matcher(LogFileParser.trim(parent.getLookahead().text));
            if (matcher.matches()) {
                parent.consumeLine();

                DiffParser.ChangeCommand changeCommand = parseCommand(matcher);
                switch (changeCommand.cmd) {
                    case 'a': {
                        int rightStart = parent.getLookahead().lineNr;
                        List<String> rLines = parseRight(changeCommand);
                        diffResult.add(new DiffChunk(changeCommand, Collections.emptyList(), null, rLines, loc(rightStart)));
                        break;
                    }
                    case 'c': {
                        int leftStart = parent.getLookahead().lineNr;
                        List<String> lLines = parseLeft(changeCommand);
                        parent.expect("---");
                        int rightStart = parent.getLookahead().lineNr;
                        List<String> rLines = parseRight(changeCommand);
                        diffResult.add(new DiffChunk(changeCommand, lLines, loc(leftStart), rLines, loc(rightStart)));
                        break;
                    }
                    case 'd': {
                        int leftStart = parent.getLookahead().lineNr;
                        List<String> lLines = parseLeft(changeCommand);
                        diffResult.add(new DiffChunk(changeCommand, lLines, loc(leftStart), Collections.emptyList(), null));
                        break;
                    }
                    default:
                        throw new LogFileParseException("Unknown diff command: ");
                }
            } else {
                // no more diff chunks; exit loop
                break;
            }
        }
        return diffResult;
    }

    private List<String> parseRight(DiffParser.ChangeCommand parseCommand) throws IOException {
        return parseSide(">", parseCommand.rFrom, parseCommand.rTo);
    }

    private List<String> parseLeft(DiffParser.ChangeCommand parseCommand) throws IOException {
        return parseSide("<", parseCommand.lFrom, parseCommand.lTo);
    }

    private List<String> parseSide(String prefix, int from, int to) throws IOException {
        List<String> lines = new LinkedList<>();
        int len = to == -1 ? 1 : to - from + 1;
        for (int i = 0; i < len; i++) {
            parent.expect(prefix);
            lines.add(LogFileParser.trim(LogFileParser.trim(parent.getCurLine().text).substring(prefix.length())));
        }
        return lines;
    }

    private ChangeCommand parseCommand(Matcher matcher) {
        if (matcher.matches()) {
            String cmdStr = matcher.group("CMD");
            if (cmdStr.length() != 1) {
                throw new LogFileParseException("Invalid diff change command: " + cmdStr);
            }

            char cmd = cmdStr.charAt(0);

            String lFromStr = matcher.group(1);
            String lToStr = matcher.group(3);
            String rFromStr = matcher.group(5);
            String rToStr = matcher.group(7);

            return new ChangeCommand(atoi(lFromStr), atoi(lToStr), cmd, atoi(rFromStr), atoi(rToStr));
        }
        throw new LogFileParseException("Invalid diff change command: " + parent.getCurLine().text);
    }

    private static int atoi(String rToStr) {
        if (rToStr != null && !rToStr.isEmpty()) {
            return Integer.parseInt(rToStr);
        }
        return -1;
    }

    public static class DiffChunk {
        private final DiffParser.ChangeCommand cmd;
        private final List<String> left;
        private final Location leftStart;
        private final List<String> right;
        private final Location rightStart;

        protected DiffChunk(DiffParser.ChangeCommand cmd, List<String> left, Location leftStart, List<String> right, Location rightStart) {
            this.cmd = cmd;
            this.left = left;
            this.right = right;
            this.leftStart = leftStart;
            this.rightStart = rightStart;
        }

        public DiffParser.ChangeCommand getCmd() {
            return cmd;
        }

        public List<String> getLeft() {
            return left;
        }

        public List<String> getRight() {
            return right;
        }

        public Path getLeftFile() {
            return leftStart != null ? leftStart.file : null;
        }

        public int getLeftStartLine() {
            return leftStart != null ? leftStart.lineNr : -1;
        }

        public Path getRightFile() {
            return rightStart != null ? rightStart.file : null;
        }

        public int getRightStartLine() {
            return rightStart != null ? rightStart.lineNr : -1;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append(cmd).append(System.lineSeparator());
            switch (cmd.cmd) {
                case 'a':
                    right.stream().forEach(r -> sb.append("> ").append(r).append(System.lineSeparator()));
                    break;
                case 'c':
                    left.stream().forEach(l -> sb.append("< ").append(l).append(System.lineSeparator()));
                    sb.append("---").append(System.lineSeparator());
                    right.stream().forEach(r -> sb.append("> ").append(r).append(System.lineSeparator()));
                    break;
                case 'd':
                    left.stream().forEach(l -> sb.append("< ").append(l).append(System.lineSeparator()));
                    break;
            }
            return sb.toString();
        }

        public Location getLocation() {
            if (leftStart != null) {
                return leftStart;
            }
            assert rightStart != null;
            return rightStart;
        }
    }

    public static class ChangeCommand {

        public final int lFrom;
        public final int lTo;
        public final char cmd;
        public final int rFrom;
        public final int rTo;

        protected ChangeCommand(int lFrom, int lTo, char cmd, int rFrom, int rTo) {
            this.lFrom = lFrom;
            this.lTo = lTo;
            this.cmd = cmd;
            this.rFrom = rFrom;
            this.rTo = rTo;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(lFrom);
            if (lTo != -1) {
                sb.append(',').append(lTo);
            }
            sb.append(cmd).append(rFrom);
            if (rTo != -1) {
                sb.append(',').append(rTo);
            }
            return sb.toString();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + cmd;
            result = prime * result + lFrom;
            result = prime * result + lTo;
            result = prime * result + rFrom;
            result = prime * result + rTo;
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ChangeCommand other = (ChangeCommand) obj;
            if (cmd != other.cmd) {
                return false;
            }
            if (lFrom != other.lFrom) {
                return false;
            }
            if (lTo != other.lTo) {
                return false;
            }
            if (rFrom != other.rFrom) {
                return false;
            }
            if (rTo != other.rTo) {
                return false;
            }
            return true;
        }

    }

}
