#
# Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
  "mxversion" : "3.4.0",
  "name" : "fastr",

  "imports" : {
    "suites" : {
        "list" : [
            ["graal", "b09503284ac83d46a51128eb6e8ab0fd502838a0", "http://hg.openjdk.java.net/graal/graal"],
        ],
      },
   },

  # distributions that we depend on
  "libraries" : {
        "TRUFFLE" : {
            "kind" : "distribution"
        },

        "GRAAL" : {
            "kind" : "distribution"
        },

        "JVMCI_API" : {
            "kind" : "distribution"
        },

        "TRUFFLE-DSL-PROCESSOR" : {
            "kind" : "distribution",
            "dependencies" : ["TRUFFLE"],
        },

    "GNUR" : {
        "path" : "lib/R-3.1.3.tar.gz",
        "urls" : ["http://cran.rstudio.com/src/base/R-3/R-3.1.3.tar.gz"],
        "sha1" : "2c9165060b91e45ac73d8cb7507ee9e52816f8b3"
    },

    "GNU_ICONV" : {
        "path" : "lib/libiconv-1.14.tar.gz",
        "urls" : ["http://ftp.gnu.org/pub/gnu/libiconv/libiconv-1.14.tar.gz"],
        "sha1" : "be7d67e50d72ff067b2c0291311bc283add36965"
    },

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
      "path" : "lib/jnr-posix-3.0.10.jar",
      "urls" : [
        "http://lafo.ssw.uni-linz.ac.at/graal-external-deps/jnr-posix-3.0.10.jar",
        "https://search.maven.org/remotecontent?filepath=com/github/jnr/jnr-posix/3.0.10/jnr-posix-3.0.10.jar",
      ],
      "sha1" : "3354732f2922db2b33342345b059381161b8d85f",
    },

    "JNR_CONSTANTS" : {
      "path" : "lib/jnr-constants-0.8.6.jar",
      "urls" : [
        "http://lafo.ssw.uni-linz.ac.at/graal-external-deps/jnr-constants-0.8.6.jar",
        "https://search.maven.org/remotecontent?filepath=com/github/jnr/jnr-constants/0.8.6/jnr-constants-0.8.6.jar",
      ],
      "sha1" : "692b0031a2854988431f57581e4058bf5cee7c8b",
    },

    "JNR_FFI" : {
      "path" : "lib/jnr-ffi-2.0.2.jar",
      "urls" : [
        "http://lafo.ssw.uni-linz.ac.at/graal-external-deps/jnr-ffi-2.0.2.jar",
        "https://search.maven.org/remotecontent?filepath=com/github/jnr/jnr-ffi/2.0.2/jnr-ffi-2.0.2.jar",
      ],
      "sha1" : "9161bdb3007cdff94d7b2843bdb5d0e8fb209bf1",
    },

    "JFFI" : {
      "path" : "lib/jffi-1.2.8.jar",
      "urls" : [
        "http://lafo.ssw.uni-linz.ac.at/graal-external-deps/jffi-1.2.8.jar",
        "https://search.maven.org/remotecontent?filepath=com/github/jnr/jffi/1.2.8/jffi-1.2.8.jar",
      ],
      "sha1" : "bb04c5cf07c114dfdb96a807a92e209edc884822",
    },

    "JFFI_NATIVE" : {
      "path" : "lib/jffi-1.2.8-native.jar",
      "urls" : [
        "http://lafo.ssw.uni-linz.ac.at/graal-external-deps/jffi-1.2.8-native.jar",
        "https://search.maven.org/remotecontent?filepath=com/github/jnr/jffi/1.2.8/jffi-1.2.8-native.jar",
      ],
      "sha1" : "043462be96ce1dd3105b928c0271b056dbe4d75d",
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

    "com.oracle.truffle.r.nodes" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.api.dsl",
        "com.oracle.truffle.r.parser",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "1.8",
      "annotationProcessors" : [
          "TRUFFLE-DSL-PROCESSOR",
      ],
      "workingSets" : "Truffle,FastR",
      "jacoco" : "include",
    },

    "com.oracle.truffle.r.nodes.builtin" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.r.nodes",
        "com.oracle.truffle.r.library",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "1.8",
      "annotationProcessors" : [
        "TRUFFLE-DSL-PROCESSOR",
      ],
      "workingSets" : "Truffle,FastR",
      "jacoco" : "include",
    },

    "com.oracle.truffle.r.nodes.test" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "JUNIT",
        "com.oracle.truffle.r.test",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle,FastR,Test",
      "jacoco" : "include",
    },

    "com.oracle.truffle.r.test" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "JUNIT",
        "com.oracle.truffle.r.engine",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle,FastR,Test",
      "jacoco" : "include",
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
      "jacoco" : "include",
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
      "jacoco" : "include",
    },

    "com.oracle.truffle.r.runtime" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "TRUFFLE",
        "FINDBUGS",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle,FastR",
      "jacoco" : "include",
    },

    "com.oracle.truffle.r.runtime.ffi" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.r.runtime",
        "JVMCI_API",
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
      "jacoco" : "include",
    },

    "com.oracle.truffle.r.native" : {
      "sourceDirs" : [],
      "dependencies" : [
        "GNUR",
        "GNU_ICONV",
      ],
      "native" : "true",
      "workingSets" : "FastR",
    },

    "com.oracle.truffle.r.library" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.r.nodes",
        "com.oracle.truffle.r.runtime",
        "com.oracle.truffle.r.runtime.ffi",
      ],
      "annotationProcessors" : [
          "com.oracle.truffle.dsl.processor",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "1.8",
      "workingSets" : "FastR",
      "jacoco" : "include",

    },

      "com.oracle.truffle.r.repl": {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.tools.debug.engine",
        "com.oracle.truffle.tools.debug.shell",
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
        "GNUR",
        "GNU_ICONV",
      ],
      "distDependencies" : [
        "TRUFFLE",
        "GRAAL",
      ],
    }
  },

}
