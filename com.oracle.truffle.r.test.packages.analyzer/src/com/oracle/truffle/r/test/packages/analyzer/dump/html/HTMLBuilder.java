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
package com.oracle.truffle.r.test.packages.analyzer.dump.html;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A simple class for building and dumping HTML files.
 */
public class HTMLBuilder {

    private PrintWriter pw;
    private Tag root;

    public HTMLBuilder(PrintWriter pw) {
        this.pw = pw;
    }

    protected void dumpPrologue() {

        pw.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
    }

    public Tag html(Tag... children) {
        if (root != null) {
            throw new RuntimeException("HTML root tag already used!");
        }
        return (root = generic("html", children));
    }

    public Tag head(Tag... children) {
        return generic("head", children);
    }

    public Tag title(String title) {
        return generic("title", title);
    }

    public Tag body(Tag... children) {
        return generic("body", null, children);
    }

    public Tag table(Tag... children) {
        return generic("table", children);
    }

    public Tag tr(Tag... children) {
        return generic("tr", children);
    }

    public Tag td(String content) {
        return generic("td", content);
    }

    public Tag td(Tag... children) {
        return generic("td", children);
    }

    public Tag a(String href, String content) {
        Tag a = generic("a", content);
        a.addAttribute("href", href);
        return a;
    }

    public Tag h1(String content) {
        return generic("h1", content);
    }

    public Tag h2(String content) {
        return generic("h2", content);
    }

    public Tag q(String content) {
        return generic("q", content);
    }

    public Tag generic(String key, String content, Tag... children) {
        return new Tag(key, content, Arrays.stream(children).collect(Collectors.toList()));
    }

    public Tag generic(String key, Tag... children) {
        return generic(key, null, children);
    }

    public Tag generic(String key, String content) {
        return new Tag(key, content);
    }

    public String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\t", "    ").replace(" ", "&#8203;&nbsp;&#8203;").replace("\n", "<br />").replace("\r\n", "<br />");
    }

    /**
     * Recursively dumps all generated tags starting at the root tag.
     */
    public void dump() {
        if (root == null) {
            throw new RuntimeException("no HTML root tag generated");
        }
        dumpPrologue();
        visit(root);
    }

    private void visit(Tag t) {

        pw.println(t.getOpening());
        if (t.content != null) {
            pw.print(t.content);
        } else {
            t.children.stream().forEach(child -> visit(child));
        }
        pw.println(t.getClosing());
    }

    public static class Tag {

        private String name;
        private String content;
        private Map<String, String> attributes = new HashMap<>();
        private List<Tag> children = new LinkedList<>();

        protected Tag(String name) {
            this.name = name;
        }

        protected Tag(String name, String content) {
            this.name = name;
            this.content = content;
        }

        protected Tag(String name, String content, List<Tag> children) {
            this.name = name;
            this.content = content;
            this.children = children;
        }

        String getOpening() {
            StringBuilder sb = new StringBuilder();
            sb.append('<').append(name);
            for (String key : attributes.keySet()) {
                sb.append(' ').append(key).append("=\"").append(attributes.get(key)).append('"');
            }
            sb.append('>');
            return sb.toString();
        }

        String getClosing() {
            return "</" + name + ">";
        }

        public void addChild(Tag child) {
            children.add(child);
        }

        public void addAttribute(String key, String value) {
            attributes.put(key, value);
        }
    }

}
