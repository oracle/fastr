/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 3 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.oracle.truffle.r.runtime;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Predicate;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.TruffleLanguage.Env;
import com.oracle.truffle.r.runtime.RError.Message;
import java.nio.file.NoSuchFileException;
import java.util.logging.Level;

public class FileSystemUtils {
    private static PosixFilePermission[] permissionValues = PosixFilePermission.values();

    public static Set<PosixFilePermission> permissionsFromMode(int mode) {
        Set<PosixFilePermission> permissions = EnumSet.noneOf(PosixFilePermission.class);
        for (int i = 0; i < permissionValues.length; i++) {
            if ((mode & (1 << (permissionValues.length - i - 1))) != 0) {
                permissions.add(permissionValues[i]);
            }
        }
        return permissions;
    }

    @TruffleBoundary
    public static int chmod(TruffleFile path, int mode) {
        try {
            path.setPosixPermissions(permissionsFromMode(mode));
            return mode;
        } catch (IOException e) {
            throw RError.error(RError.NO_CALLER, Message.GENERIC, "Cannot change file permissions.");
        }
    }

    @TruffleBoundary
    public static void mkdir(TruffleFile dir, int mode) throws IOException {
        Set<PosixFilePermission> permissions = permissionsFromMode(mode);
        dir.createDirectory(PosixFilePermissions.asFileAttribute(permissions));
    }

    @TruffleBoundary
    public static boolean deleteIfExists(TruffleFile f) throws IOException {
        try {
            f.delete();
            return true;
        } catch (FileNotFoundException ex) {
            return false;
        }
    }

    public static List<String> readAllLines(TruffleFile file) throws IOException {
        return readAllLines(file, StandardCharsets.UTF_8);
    }

    public static List<String> readAllLines(TruffleFile file, Charset cs) throws IOException {
        try (BufferedReader reader = file.newBufferedReader(cs)) {
            List<String> result = new ArrayList<>();
            for (;;) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                result.add(line);
            }
            return result;
        }
    }

    /**
     * Find that does not follow links and with a matcher not getting attributes as a parameter.
     *
     * @param start starting truffle file.
     * @param maxDepth maximum number of directory levels to search.
     * @param matcher function used to decide whether a file should be included in the returned
     *            stream.
     * @return stream of truffle files.
     * @throws IOException if an I/O error occurs.
     */
    public static Stream<TruffleFile> find(TruffleFile start, int maxDepth, Predicate<TruffleFile> matcher) throws IOException {
        FileTreeIterator iterator = new FileTreeIterator(start, maxDepth);
        try {
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.DISTINCT), false).onClose(iterator::close).filter(entry -> matcher.test(entry.file())).map(
                            entry -> entry.file());
        } catch (Error | RuntimeException e) {
            iterator.close();
            throw e;
        }
    }

    public static Stream<TruffleFile> walk(TruffleFile start, int maxDepth)
                    throws IOException {
        FileTreeIterator iterator = new FileTreeIterator(start, maxDepth);
        try {
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.DISTINCT), false).onClose(iterator::close).map(entry -> entry.file());
        } catch (Error | RuntimeException e) {
            iterator.close();
            throw e;
        }
    }

    public static TruffleFile walkFileTree(TruffleFile start, FileVisitor<TruffleFile> visitor) throws IOException {
        start.visit(visitor, Integer.MAX_VALUE);
        return start;
    }

    /**
     * Walk file tree without possibility to follow links and with file attributes being null (those
     * passed into {@link FileVisitor}).
     *
     * @param start starting truffle file.
     * @param maxDepth maximum number of directory levels to search.
     * @param visitor file visitor to invoke for each file.
     * @return the starting file.
     * @throws IOException if an I/O error is thrown by a visitor method.
     */
    public static TruffleFile walkFileTree(TruffleFile start, int maxDepth, FileVisitor<TruffleFile> visitor) throws IOException {
        start.visit(visitor, maxDepth);
        return start;
    }

    private static final class FileTreeIterator implements Iterator<Event>, Closeable {
        private final FileTreeWalker walker;
        private Event next;

        FileTreeIterator(TruffleFile start, int maxDepth) throws IOException {
            this.walker = new FileTreeWalker(maxDepth);
            this.next = walker.walk(start);
            assert next.type() == EventType.ENTRY ||
                            next.type() == EventType.START_DIRECTORY;

            // IOException if there a problem accessing the starting file
            IOException ioe = next.ioeException();
            if (ioe != null) {
                throw ioe;
            }
        }

        private void fetchNextIfNeeded() {
            if (next == null) {
                Event ev = walker.next();
                while (ev != null) {
                    IOException ioe = ev.ioeException();
                    if (ioe != null) {
                        throw new UncheckedIOException(ioe);
                    }

                    // END_DIRECTORY events are ignored
                    if (ev.type() != EventType.END_DIRECTORY) {
                        next = ev;
                        return;
                    }
                    ev = walker.next();
                }
            }
        }

        @Override
        public boolean hasNext() {
            if (!walker.isOpen()) {
                throw new IllegalStateException();
            }
            fetchNextIfNeeded();
            return next != null;
        }

        @Override
        public Event next() {
            if (!walker.isOpen()) {
                throw new IllegalStateException();
            }
            fetchNextIfNeeded();
            if (next == null) {
                throw new NoSuchElementException();
            }
            Event result = next;
            next = null;
            return result;
        }

        @Override
        public void close() {
            walker.close();
        }
    }

    private static final class FileTreeWalker implements Closeable {
        private final int maxDepth;
        private final ArrayDeque<DirectoryNode> stack = new ArrayDeque<>();
        private boolean closed;

        FileTreeWalker(int maxDepth) {
            this.maxDepth = maxDepth;
        }

        private Event visit(TruffleFile entry, boolean ignoreSecurityException) {
            // at maximum depth or file is not a directory
            int depth = stack.size();
            if (depth >= maxDepth || !entry.isDirectory()) {
                return new Event(EventType.ENTRY, entry);
            }

            // file is a directory, attempt to open it
            DirectoryStream<TruffleFile> stream = null;
            try {
                stream = entry.newDirectoryStream();
            } catch (IOException ioe) {
                return new Event(EventType.ENTRY, entry, ioe);
            } catch (SecurityException se) {
                if (ignoreSecurityException) {
                    return null;
                }
                throw se;
            }

            // push a directory node to the stack and return an event
            stack.push(new DirectoryNode(entry, stream));
            return new Event(EventType.START_DIRECTORY, entry);
        }

        Event walk(TruffleFile file) {
            if (closed) {
                throw new IllegalStateException("Closed");
            }

            Event ev = visit(file, false);
            assert ev != null;
            return ev;
        }

        Event next() {
            DirectoryNode top = stack.peek();
            if (top == null) {
                return null;      // stack is empty, we are done
            }

            // continue iteration of the directory at the top of the stack
            Event ev;
            do {
                TruffleFile entry = null;
                IOException ioe = null;

                // get next entry in the directory
                if (!top.skipped()) {
                    Iterator<TruffleFile> iterator = top.iterator();
                    try {
                        if (iterator.hasNext()) {
                            entry = iterator.next();
                        }
                    } catch (DirectoryIteratorException x) {
                        ioe = x.getCause();
                    }
                }

                // no next entry so close and pop directory, creating corresponding event
                if (entry == null) {
                    try {
                        top.stream().close();
                    } catch (IOException e) {
                        if (ioe == null) {
                            ioe = e;
                        } else {
                            ioe.addSuppressed(e);
                        }
                    }
                    stack.pop();
                    return new Event(EventType.END_DIRECTORY, top.directory(), ioe);
                }

                // visit the entry
                ev = visit(entry, true);

            } while (ev == null);

            return ev;
        }

        void pop() {
            if (!stack.isEmpty()) {
                DirectoryNode node = stack.pop();
                try {
                    node.stream().close();
                } catch (IOException ignore) {
                }
            }
        }

        boolean isOpen() {
            return !closed;
        }

        @Override
        public void close() {
            if (!closed) {
                while (!stack.isEmpty()) {
                    pop();
                }
                closed = true;
            }
        }
    }

    private static final class DirectoryNode {
        private final TruffleFile dir;
        private final DirectoryStream<TruffleFile> stream;
        private final Iterator<TruffleFile> iterator;
        private boolean skipped;

        DirectoryNode(TruffleFile dir, DirectoryStream<TruffleFile> stream) {
            this.dir = dir;
            this.stream = stream;
            this.iterator = stream.iterator();
        }

        TruffleFile directory() {
            return dir;
        }

        DirectoryStream<TruffleFile> stream() {
            return stream;
        }

        Iterator<TruffleFile> iterator() {
            return iterator;
        }

        @SuppressFBWarnings(value = "UWF_UNWRITTEN_FIELD", justification = "incomplete implementation")
        boolean skipped() {
            return skipped;
        }
    }

    private enum EventType {
        START_DIRECTORY,
        END_DIRECTORY,
        ENTRY;
    }

    private static final class Event implements BasicFileAttributes {
        private final EventType type;
        private final TruffleFile file;
        private final IOException ioe;

        private Event(EventType type, TruffleFile file, IOException ioe) {
            this.type = type;
            this.file = file;
            this.ioe = ioe;
        }

        Event(EventType type, TruffleFile file) {
            this(type, file, null);
        }

        EventType type() {
            return type;
        }

        TruffleFile file() {
            return file;
        }

        IOException ioeException() {
            return ioe;
        }

        @Override
        public FileTime lastModifiedTime() {
            try {
                return file.getLastModifiedTime();
            } catch (IOException ex) {
                return null;
            }
        }

        @Override
        public FileTime lastAccessTime() {
            try {
                return file.getLastAccessTime();
            } catch (IOException ex) {
                return null;
            }
        }

        @Override
        public FileTime creationTime() {
            try {
                return file.getCreationTime();
            } catch (IOException ex) {
                return null;
            }
        }

        @Override
        public boolean isRegularFile() {
            return file.isRegularFile();
        }

        @Override
        public boolean isDirectory() {
            return file.isDirectory();
        }

        @Override
        public boolean isSymbolicLink() {
            return file.isSymbolicLink();
        }

        @Override
        public boolean isOther() {
            return !(isRegularFile() || isDirectory() || isSymbolicLink());
        }

        @Override
        public long size() {
            try {
                return file.size();
            } catch (IOException ex) {
                return 0;
            }
        }

        @Override
        public Object fileKey() {
            return null;
        }
    }

    private static final String regexMetaChars = ".^$+{[]|()";
    private static final String globMetaChars = "\\*?[{";

    private static boolean isRegexMeta(char c) {
        return regexMetaChars.indexOf(c) != -1;
    }

    private static boolean isGlobMeta(char c) {
        return globMetaChars.indexOf(c) != -1;
    }

    private static char EOL = 0;

    private static char next(String glob, int i) {
        if (i < glob.length()) {
            return glob.charAt(i);
        }
        return EOL;
    }

    private static String toRegexPattern(String globPattern, boolean isDos) {
        boolean inGroup = false;
        StringBuilder regex = new StringBuilder("^");

        int i = 0;
        while (i < globPattern.length()) {
            char c = globPattern.charAt(i++);
            switch (c) {
                case '\\':
                    // escape special characters
                    if (i == globPattern.length()) {
                        throw new PatternSyntaxException("No character to escape",
                                        globPattern, i - 1);
                    }
                    char next = globPattern.charAt(i++);
                    if (isGlobMeta(next) || isRegexMeta(next)) {
                        regex.append('\\');
                    }
                    regex.append(next);
                    break;
                case '/':
                    if (isDos) {
                        regex.append("\\\\");
                    } else {
                        regex.append(c);
                    }
                    break;
                case '[':
                    // don't match name separator in class
                    if (isDos) {
                        regex.append("[[^\\\\]&&[");
                    } else {
                        regex.append("[[^/]&&[");
                    }
                    if (next(globPattern, i) == '^') {
                        // escape the regex negation char if it appears
                        regex.append("\\^");
                        i++;
                    } else {
                        // negation
                        if (next(globPattern, i) == '!') {
                            regex.append('^');
                            i++;
                        }
                        // hyphen allowed at start
                        if (next(globPattern, i) == '-') {
                            regex.append('-');
                            i++;
                        }
                    }
                    boolean hasRangeStart = false;
                    char last = 0;
                    while (i < globPattern.length()) {
                        c = globPattern.charAt(i++);
                        if (c == ']') {
                            break;
                        }
                        if (c == '/' || (isDos && c == '\\')) {
                            throw new PatternSyntaxException("Explicit 'name separator' in class",
                                            globPattern, i - 1);
                        }
                        // TBD: how to specify ']' in a class?
                        if (c == '\\' || c == '[' ||
                                        c == '&' && next(globPattern, i) == '&') {
                            // escape '\', '[' or "&&" for regex class
                            regex.append('\\');
                        }
                        regex.append(c);

                        if (c == '-') {
                            if (!hasRangeStart) {
                                throw new PatternSyntaxException("Invalid range",
                                                globPattern, i - 1);
                            }
                            if ((c = next(globPattern, i++)) == EOL || c == ']') {
                                break;
                            }
                            if (c < last) {
                                throw new PatternSyntaxException("Invalid range",
                                                globPattern, i - 3);
                            }
                            regex.append(c);
                            hasRangeStart = false;
                        } else {
                            hasRangeStart = true;
                            last = c;
                        }
                    }
                    if (c != ']') {
                        throw new PatternSyntaxException("Missing ']", globPattern, i - 1);
                    }
                    regex.append("]]");
                    break;
                case '{':
                    if (inGroup) {
                        throw new PatternSyntaxException("Cannot nest groups",
                                        globPattern, i - 1);
                    }
                    regex.append("(?:(?:");
                    inGroup = true;
                    break;
                case '}':
                    if (inGroup) {
                        regex.append("))");
                        inGroup = false;
                    } else {
                        regex.append('}');
                    }
                    break;
                case ',':
                    if (inGroup) {
                        regex.append(")|(?:");
                    } else {
                        regex.append(',');
                    }
                    break;
                case '*':
                    if (next(globPattern, i) == '*') {
                        // crosses directory boundaries
                        regex.append(".*");
                        i++;
                    } else {
                        // within directory boundary
                        if (isDos) {
                            regex.append("[^\\\\]*");
                        } else {
                            regex.append("[^/]*");
                        }
                    }
                    break;
                case '?':
                    if (isDos) {
                        regex.append("[^\\\\]");
                    } else {
                        regex.append("[^/]");
                    }
                    break;

                default:
                    if (isRegexMeta(c)) {
                        regex.append('\\');
                    }
                    regex.append(c);
            }
        }

        if (inGroup) {
            throw new PatternSyntaxException("Missing '}", globPattern, i - 1);
        }

        return regex.append('$').toString();
    }

    public static String toUnixRegexPattern(String globPattern) {
        return toRegexPattern(globPattern, false);
    }

    public static TruffleFile getSafeTruffleFile(Env env, String path) {
        TruffleFile origFile = env.getInternalTruffleFile(path);

        TruffleFile f = origFile;
        try {
            origFile = env.getInternalTruffleFile(path);
            if (origFile.exists()) {
                try {
                    f = origFile.getCanonicalFile();
                } catch (NoSuchFileException e) {
                    // absolute path "exists", but cannonical does not
                    // happens e.g. during install.packages: file.exists("/{rHome}/inst/..")
                    // lets optimistically FALLBACK on absolute path
                }
            }
        } catch (IOException e) {
            RLogger.getLogger(RLogger.LOGGER_FILE_ACCEESS).log(Level.SEVERE, "Unable to access file " + path + " " + e.getMessage(), e);
            throw RError.error(RError.SHOW_CALLER, Message.FILE_OPEN_ERROR);
        }

        final TruffleFile home = REnvVars.getRHomeTruffleFile(env);
        if (f.startsWith(home) && isLibraryFile(home.relativize(f))) {
            return origFile;
        } else {
            try {
                return env.getPublicTruffleFile(path);
            } catch (SecurityException e) {
                RLogger.getLogger(RLogger.LOGGER_FILE_ACCEESS).log(Level.SEVERE, "Unable to access file " + path + " " + e.getMessage(), e);
                throw RError.error(RError.SHOW_CALLER, Message.FILE_OPEN_ERROR);
            }
        }
    }

    private static boolean isLibraryFile(TruffleFile relativePathFromHome) {
        final String fileName = relativePathFromHome.getName();
        if (fileName == null) {
            return false;
        }
        return relativePathFromHome.startsWith("library");
    }

}
