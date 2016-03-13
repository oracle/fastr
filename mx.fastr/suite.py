#
# Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
  "mxversion" : "5.8.1",
  "name" : "fastr",
  "imports" : {
    "suites" : [
            {
               "name" : "truffle",
               "version" : "946de8ed59266b2c5cf7ab101c62a5d8c22853d7",
               "urls" : [
                    {"url" : "https://github.com/graalvm/truffle.git", "kind" : "git"},
                    {"url" : "https://curio.ssw.jku.at/nexus/content/repositories/snapshots", "kind" : "binary"},
                ]
            },
            {
               "name" : "graal-core",
               "version" : "a0e54263b5001f68e25e329f9ebc9f3444a880c7",
               "urls" : [
                    {"url" : "https://github.com/graalvm/graal-core", "kind" : "git"},
                    {"url" : "https://curio.ssw.jku.at/nexus/content/repositories/snapshots", "kind" : "binary"},
                ]
            },

        ],
   },

  # libraries that we depend on
  # N.B. The first four with a "path" attribute must be located
  # relative to the suite root and not the mx cache
  "libraries" : {
    "GNUR" : {
        "path" : "libdownloads/R-3.2.4.tar.gz",
        "urls" : ["http://cran.rstudio.com/src/base/R-3/R-3.2.4.tar.gz"],
        "sha1" : "632664b3caa8d39f5fe6ac2ee9611b0f89ad6ed9",
        "resource" : "true"
    },

    "GNU_ICONV" : {
        "path" : "libdownloads/libiconv-1.14.tar.gz",
        "urls" : ["http://ftp.gnu.org/pub/gnu/libiconv/libiconv-1.14.tar.gz"],
        "sha1" : "be7d67e50d72ff067b2c0291311bc283add36965",
        "resource" : "true"
    },

    "ANTLR-3.5" : {
      "path" : "libdownloads/antlr-runtime-3.5.jar",
      "urls" : ["http://central.maven.org/maven2/org/antlr/antlr-runtime/3.5/antlr-runtime-3.5.jar"],
      "sha1" : "0baa82bff19059401e90e1b90020beb9c96305d7",
    },

    "JDK_TOOLS" : {
      "path" : "${JAVA_HOME}/lib/tools.jar",
      "sha1" : "NOCHECK",
    },

    "ANTLR-C-3.5" : {
      "path" : "libdownloads/antlr-complete-3.5.1.jar",
      "urls" : ["http://central.maven.org/maven2/org/antlr/antlr-complete/3.5.1/antlr-complete-3.5.1.jar"],
      "sha1" : "ebb4b995fd67a9b291ea5b19379509160f56e154",
    },


    "NETLIB" : {
      "urls" : ["http://central.maven.org/maven2/com/googlecode/netlib-java/netlib-java/0.9.3/netlib-java-0.9.3.jar"],
      "sha1" : "1d41b60e5180f6bcb7db15e7353dde7147cd3928",
    },

    "JNR_POSIX" : {
      "urls" : [
        "https://lafo.ssw.uni-linz.ac.at/pub/graal-external-deps/jnr-posix-3.0.18.jar",
        "https://search.maven.org/remotecontent?filepath=com/github/jnr/jnr-posix/3.0.18/jnr-posix-3.0.18.jar",
      ],
      "sha1" : "96ae4f7837eeda0e47a470570da1803941f1bf78",
    },

    "JNR_CONSTANTS" : {
      "urls" : [
        "https://lafo.ssw.uni-linz.ac.at/pub/graal-external-deps/jnr-constants-0.9.0.jar",
        "https://search.maven.org/remotecontent?filepath=com/github/jnr/jnr-constants/0.9.0/jnr-constants-0.9.0.jar",
      ],
      "sha1" : "6894684e17a84cd500836e82b5e6c674b4d4dda6",
    },

    "JNR_FFI" : {
      "urls" : [
        "https://search.maven.org/remotecontent?filepath=com/github/jnr/jnr-ffi/2.0.7/jnr-ffi-2.0.7.jar",
      ],
      "sourcePath" : "https://search.maven.org/remotecontent?filepath=com/github/jnr/jnr-ffi/2.0.7/jnr-ffi-2.0.7-sources.jar",
      "sha1" : "f0968c5bb5a283ebda2df3604c2c1129d45196e3",
      "sourceSha1" : "164d820d7c38c14cd3675bfbf3eb6aeeedc9b0f4",
    },

    "JFFI" : {
      "urls" : [
        "https://lafo.ssw.uni-linz.ac.at/pub/graal-external-deps/jffi-1.2.9.jar",
        "https://search.maven.org/remotecontent?filepath=com/github/jnr/jffi/1.2.9/jffi-1.2.9.jar",
      ],
      "sha1" : "93294973285d2977c41fb1cfcfa864d41b145498",
    },

    "JFFI_NATIVE" : {
      "urls" : [
        "https://lafo.ssw.uni-linz.ac.at/pub/graal-external-deps/jffi-1.2.9-native.jar",
        "https://search.maven.org/remotecontent?filepath=com/github/jnr/jffi/1.2.9/jffi-1.2.9-native.jar",
      ],
      "sha1" : "1b1e82f867bb47237e6db690fbc6bfc999689c63",
    },

    "JNR_INVOKE" : {
      "urls" : ["https://search.maven.org/remotecontent?filepath=com/github/jnr/jnr-invoke/0.1/jnr-invoke-0.1.jar"],
      "sha1" : "d0f846c3d3cb98dfd5e2bbd3cca236337fb0afa1",
    },

    "JNR_UDIS86" : {
      "urls" : ["https://search.maven.org/remotecontent?filepath=com/github/jnr/jnr-udis86/0.1/jnr-udis86-0.1.jar"],
      "sha1" : "88accfa82203ea74a4a82237061c28ac8b4224af",
    },

    "JNR_X86ASM" : {
      "urls" : [
        "https://lafo.ssw.uni-linz.ac.at/pub/graal-external-deps/jnr-x86asm-1.0.2.jar",
        "https://search.maven.org/remotecontent?filepath=com/github/jnr/jnr-x86asm/1.0.2/jnr-x86asm-1.0.2.jar",
      ],
      "sha1" : "006936bbd6c5b235665d87bd450f5e13b52d4b48",
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


  },

  "projects" : {
    "com.oracle.truffle.r.parser.processor" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "JDK_TOOLS",
        "ANTLR-3.5",
        "ANTLR-C-3.5",
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
      "annotationProcessors" : ["TRUFFLE_R_PARSER_PROCESSOR"],
      "workingSets" : "Truffle,FastR",
    },

    "com.oracle.truffle.r.nodes" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.r.parser",
        "truffle:TRUFFLE_DEBUG",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "1.8",
      "annotationProcessors" : [
          "truffle:TRUFFLE_DSL_PROCESSOR",
      ],
      "workingSets" : "Truffle,FastR",
      "jacoco" : "include",
    },

    "com.oracle.truffle.r.nodes.builtin" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.r.library",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "1.8",
      "annotationProcessors" : [
        "truffle:TRUFFLE_DSL_PROCESSOR",
      ],
      "workingSets" : "Truffle,FastR",
      "jacoco" : "include",
    },

    "com.oracle.truffle.r.nodes.test" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
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
        "mx:JUNIT",
        "truffle:TRUFFLE_TCK",
        "com.oracle.truffle.r.engine",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle,FastR,Test",
      "jacoco" : "include",
    },

    "com.oracle.truffle.r.test.native" : {
      "sourceDirs" : [],
      "dependencies" : ["com.oracle.truffle.r.native"],
      "native" : "true",
      "workingSets" : "FastR",
    },

    "com.oracle.truffle.r.test.cran" : {
      "sourceDirs" : ["r"],
      "javaCompliance" : "1.8",
      "workingSets" : "FastR",
    },

    "com.oracle.truffle.r.engine" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.r.nodes.builtin",
        "truffle:JLINE",
        "truffle:TRUFFLE_DEBUG",
      ],
      "annotationProcessors" : [
          "truffle:TRUFFLE_DSL_PROCESSOR",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "1.8",
      "workingSets" : "Truffle,FastR",
      "jacoco" : "include",
    },

    "com.oracle.truffle.r.runtime" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "truffle:TRUFFLE_API",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "1.8",
      "annotationProcessors" : [
          "truffle:TRUFFLE_DSL_PROCESSOR",
      ],
      "workingSets" : "Truffle,FastR",
      "jacoco" : "include",
    },

    "com.oracle.truffle.r.runtime.ffi" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.r.runtime",
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
      "output" : "com.oracle.truffle.r.native",
      "results" : ["platform.mk"],
      "workingSets" : "FastR",
    },

    "com.oracle.truffle.r.library" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.r.nodes",
        "com.oracle.truffle.r.runtime.ffi",
      ],
      "annotationProcessors" : [
          "truffle:TRUFFLE_DSL_PROCESSOR",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "1.8",
      "workingSets" : "FastR",
      "jacoco" : "include",

    },

  },

  "distributions" : {
    "TRUFFLE_R_PARSER_PROCESSOR" : {
      "subDir" : "truffle",
      "dependencies" : ["com.oracle.truffle.r.parser.processor"],
      "exclude" : [
        "JDK_TOOLS",
        "ANTLR-3.5",
        "ANTLR-C-3.5",
       ],

    },
    "FASTR" : {
      "dependencies" : ["com.oracle.truffle.r.engine", "com.oracle.truffle.r.runtime.ffi"],
      "mainClass" : "com.oracle.truffle.r.engine.shell.RCommand",
      "exclude" : [
        "JDK_TOOLS",
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
        "truffle:JLINE",
        "ANTLR-C-3.5",
        "ANTLR-3.5",
        "GNUR",
        "GNU_ICONV",
      ],
      "distDependencies" : [
        "truffle:TRUFFLE_API",
        "truffle:TRUFFLE_DEBUG",
        "TRUFFLE_R_PARSER_PROCESSOR",
      ],
    },

    "FASTR_NATIVE": {
        "native" : True,
        "path" : "mxbuild/dists/fastr.native.tar",
        "dependencies" : ["com.oracle.truffle.r.native"],
        "exclude" : [
        "GNUR",
        "GNU_ICONV",
        ]

    }
  },

}
