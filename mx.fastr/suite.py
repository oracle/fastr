#
# Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 2 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 2 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 2 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#
suite = {
  "mxversion" : "2.7.1",
  "name" : "fastr",
  "libraries" : {
    "JDK_TOOLS" : {
      "path" : "${JAVA_HOME}/lib/tools.jar",
      "sha1" : "NOCHECK",
    },

    "ANTLR" : {
      "path" : "lib/antlr-runtime-3.5.jar",
      "urls" : ["http://central.maven.org/maven2/org/antlr/antlr-runtime/3.5/antlr-runtime-3.5.jar"],
      "sha1" : "0baa82bff19059401e90e1b90020beb9c96305d7",
    },

    "NETLIB" : {
      "path" : "lib/netlib-java-0.9.3.jar",
      "urls" : ["http://central.maven.org/maven2/com/googlecode/netlib-java/netlib-java/0.9.3/netlib-java-0.9.3.jar"],
      "sha1" : "1d41b60e5180f6bcb7db15e7353dde7147cd3928",
    },

    "ANTLR-C" : {
      "path" : "lib/antlr-complete-3.5.1.jar",
      "urls" : ["http://central.maven.org/maven2/org/antlr/antlr-complete/3.5.1/antlr-complete-3.5.1.jar"],
      "sha1" : "ebb4b995fd67a9b291ea5b19379509160f56e154",
    },

    "JLINE" : {
      "path" : "lib/jline-2.11.jar",
      "urls" : [
        "http://lafo.ssw.uni-linz.ac.at/graal-external-deps/jline-2.11.jar",
        "https://search.maven.org/remotecontent?filepath=jline/jline/2.11/jline-2.11.jar",
      ],
      "sha1" : "9504d5e2da5d78237239c5226e8200ec21182040",
    },

    "JNR_POSIX" : {
      "path" : "lib/jnr-posix-3.0.1.jar",
      "urls" : [
        "http://lafo.ssw.uni-linz.ac.at/graal-external-deps/jnr-posix-3.0.1.jar",
        "https://search.maven.org/remotecontent?filepath=com/github/jnr/jnr-posix/3.0.1/jnr-posix-3.0.1.jar",
      ],
      "sha1" : "5ac18caed12108123c959c8acedef76ca4f28cb3",
    },

    "JNR_CONSTANTS" : {
      "path" : "lib/jnr-constants-0.8.5.jar",
      "urls" : [
        "http://lafo.ssw.uni-linz.ac.at/graal-external-deps/jnr-constants-0.8.5.jar",
        "https://search.maven.org/remotecontent?filepath=com/github/jnr/jnr-constants/0.8.5/jnr-constants-0.8.5.jar",
      ],
      "sha1" : "f84cca9e21f1f763a9eaf33de3d6a66a20ed7af0",
    },

    "JNR_FFI" : {
      "path" : "lib/jnr-ffi-1.0.10.jar",
      "urls" : [
        "http://lafo.ssw.uni-linz.ac.at/graal-external-deps/jnr-ffi-1.0.10.jar",
        "https://search.maven.org/remotecontent?filepath=com/github/jnr/jnr-ffi/1.0.10/jnr-ffi-1.0.10.jar",
      ],
      "sha1" : "646428e83a0e2ab4743091781ea98e3164c6d707",
    },

    "JFFI" : {
      "path" : "lib/jffi-1.2.7.jar",
      "urls" : [
        "http://lafo.ssw.uni-linz.ac.at/graal-external-deps/jffi-1.2.7.jar",
        "https://search.maven.org/remotecontent?filepath=com/github/jnr/jffi/1.2.7/jffi-1.2.7.jar",
      ],
      "sha1" : "acda5c46140404e08b3526f39db1504874b34b4c",
    },

    "JFFI_NATIVE" : {
      "path" : "lib/jffi-1.2.7-native.jar",
      "urls" : [
        "http://lafo.ssw.uni-linz.ac.at/graal-external-deps/jffi-1.2.7-native.jar",
        "https://search.maven.org/remotecontent?filepath=com/github/jnr/jffi/1.2.7/jffi-1.2.7-native.jar",
      ],
      "sha1" : "4e8c876383acb37da4347902a0a775aefd51de09",
    },

    "ASM" : {
      "path" : "lib/asm-5.0.3.jar",
      "urls" : [
        "http://lafo.ssw.uni-linz.ac.at/graal-external-deps/asm-5.0.3.jar",
        "https://search.maven.org/remotecontent?filepath=org/ow2/asm/asm/5.0.3/asm-5.0.3.jar",
      ],
      "sha1" : "dcc2193db20e19e1feca8b1240dbbc4e190824fa",
      "sourcePath" : "lib/asm-5.0.3-sources.jar",
      "sourceSha1" : "f0f24f6666c1a15c7e202e91610476bd4ce59368",
      "sourceUrls" : [
        "http://lafo.ssw.uni-linz.ac.at/graal-external-deps/asm-5.0.3-sources.jar",
        "https://search.maven.org/remotecontent?filepath=org/ow2/asm/asm/5.0.3/asm-5.0.3-sources.jar",
      ],
    },

    "JNR_X86ASM" : {
      "path" : "lib/jnr-x86asm-1.0.2.jar",
      "urls" : [
        "http://lafo.ssw.uni-linz.ac.at/graal-external-deps/jnr-x86asm-1.0.2.jar",
        "https://search.maven.org/remotecontent?filepath=com/github/jnr/jnr-x86asm/1.0.2/jnr-x86asm-1.0.2.jar",
      ],
      "sha1" : "006936bbd6c5b235665d87bd450f5e13b52d4b48",
    },

    "ASM_ANALYSIS" : {
      "path" : "lib/asm-analysis-4.0.jar",
      "urls" : [
        "http://lafo.ssw.uni-linz.ac.at/graal-external-deps/asm-analysis-4.0.jar",
        "https://search.maven.org/remotecontent?filepath=org/ow2/asm/asm-analysis/4.0/asm-analysis-4.0.jar",
      ],
      "sha1" : "1c45d52b6f6c638db13cf3ac12adeb56b254cdd7",
    },

    "ASM_COMMONS" : {
      "path" : "lib/asm-commons-4.0.jar",
      "urls" : [
        "http://lafo.ssw.uni-linz.ac.at/graal-external-deps/asm-commons-4.0.jar",
        "https://search.maven.org/remotecontent?filepath=org/ow2/asm/asm-commons/4.0/asm-commons-4.0.jar",
      ],
      "sha1" : "a839ec6737d2b5ba7d1878e1a596b8f58aa545d9",
    },

    "ASM_TREE" : {
      "path" : "lib/asm-tree-4.0.jar",
      "urls" : [
        "http://lafo.ssw.uni-linz.ac.at/graal-external-deps/asm-tree-4.0.jar",
        "https://search.maven.org/remotecontent?filepath=org/ow2/asm/asm-tree/4.0/asm-tree-4.0.jar",
      ],
      "sha1" : "67bd266cd17adcee486b76952ece4cc85fe248b8",
    },

    "ASM_UTIL" : {
      "path" : "lib/asm-util-4.0.jar",
      "urls" : [
        "http://lafo.ssw.uni-linz.ac.at/graal-external-deps/asm-util-4.0.jar",
        "https://search.maven.org/remotecontent?filepath=org/ow2/asm/asm-util/4.0/asm-util-4.0.jar",
      ],
      "sha1" : "d7a65f54cda284f9706a750c23d64830bb740c39",
    },

    "JNR_INVOKE" : {
      "path" : "lib/jnr-invoke-0.1.jar",
      "urls" : ["http://search.maven.org/remotecontent?filepath=com/github/jnr/jnr-invoke/0.1/jnr-invoke-0.1.jar"],
      "sha1" : "d0f846c3d3cb98dfd5e2bbd3cca236337fb0afa1",
    },

    "JNR_UDIS86" : {
      "path" : "lib/jnr-udis86-0.1.jar",
      "urls" : ["http://search.maven.org/remotecontent?filepath=com/github/jnr/jnr-udis86/0.1/jnr-udis86-0.1.jar"],
      "sha1" : "88accfa82203ea74a4a82237061c28ac8b4224af",
    }
  },

  "projects" : {
    "com.oracle.truffle.r.parser.processor" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "JDK_TOOLS",
        "ANTLR",
        "ANTLR-C",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle,FastR",
    },

    "com.oracle.truffle.r.parser" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.r.parser.processor",
        "com.oracle.truffle.r.runtime",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "1.8",
      "annotationProcessors" : ["com.oracle.truffle.r.parser.processor"],
      "workingSets" : "Truffle,FastR",
    },

    "com.oracle.truffle.r.nodes.builtin.processor" : {
      "sourceDirs" : ["src"],
      "dependencies" : ["JDK_TOOLS"],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle,FastR",
    },

    "com.oracle.truffle.r.nodes" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.api.dsl",
        "com.oracle.truffle.r.parser",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "1.8",
      "annotationProcessors" : ["com.oracle.truffle.dsl.processor"],
      "workingSets" : "Truffle,FastR",
    },

    "com.oracle.truffle.r.nodes.builtin" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.graal.debug",
        "com.oracle.truffle.r.nodes",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "1.8",
      "annotationProcessors" : [
        "com.oracle.truffle.dsl.processor",
        "com.oracle.truffle.r.nodes.builtin.processor",
      ],
      "workingSets" : "Truffle,FastR",
    },

    "com.oracle.truffle.r.test.ignore.processor" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "JUNIT",
        "JDK_TOOLS",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle,FastR",
    },

    "com.oracle.truffle.r.test" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "JUNIT",
        "com.oracle.truffle.r.engine",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "1.8",
      "annotationProcessors" : ["com.oracle.truffle.r.test.ignore.processor"],
      "workingSets" : "Truffle,FastR,Test",
    },

    "com.oracle.truffle.r.test.native" : {
      "sourceDirs" : [],
      "native" : "true",
      "workingSets" : "FastR",
    },

    "com.oracle.truffle.r.engine" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.r.nodes.builtin",
        "com.oracle.truffle.r.runtime.ffi",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle,FastR",
    },

    "com.oracle.truffle.r.shell" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.r.engine",
        "JLINE",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle,FastR",
    },

    "com.oracle.truffle.r.runtime" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.api",
        "com.oracle.truffle.r.options",
        "FINDBUGS",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle,FastR",
    },

    "com.oracle.truffle.r.runtime.ffi" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.r.runtime",
        "com.oracle.nfi",
        "com.oracle.graal.compiler.common",
        "ASM",
        "ASM_ANALYSIS",
        "JNR_POSIX",
        "ASM_UTIL",
        "JFFI",
        "JNR_FFI",
        "NETLIB",
        "JNR_CONSTANTS",
        "JFFI_NATIVE",
        "JNR_INVOKE",
        "JNR_UDIS86",
        "ASM",
        "ASM_TREE",
        "ASM_COMMONS",
        "JNR_X86ASM",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle,FastR",
    },

    "com.oracle.truffle.r.native" : {
      "sourceDirs" : [],
      "native" : "true",
      "workingSets" : "FastR",
    },

    "com.oracle.truffle.r.options" : {
      "sourceDirs" : ["src"],
      "dependencies" : ["com.oracle.graal.options"],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "1.8",
      "workingSets" : "FastR",

    },

    "com.oracle.truffle.r.debug": {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.debug",
        "com.oracle.truffle.r.shell",
        ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle,FastR",
    },

  },

  "distributions" : {
    "FASTR" : {
      "path" : "fastr.jar",
      "sourcesPath" : "fastr-sources.jar",
      "dependencies" : ["com.oracle.truffle.r.nodes"],
      "exclude" : [
        "JDK_TOOLS",
        "FINDBUGS",
        "NETLIB",
         "ASM",
        "ASM_UTIL",
        "ASM_TREE",
        "ASM_COMMONS",
        "ASM_ANALYSIS",
        "ASM",
        "JNR_X86ASM",
        "JFFI_NATIVE",
        "JFFI",
        "JNR_FFI",
        "JNR_CONSTANTS",
        "JNR_POSIX",
        "JNR_INVOKE",
        "JNR_UDIS86",
        "JLINE",
        "ANTLR-C",
        "ANTLR",
      ],
      "distDependencies" : [
        "TRUFFLE",
        "GRAAL",
      ],
    }
  },

}
