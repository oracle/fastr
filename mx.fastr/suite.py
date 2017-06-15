#
# Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
  "mxversion" : "5.60.0",
  "name" : "fastr",
  "versionConflictResolution" : "latest",
  "imports" : {
    "suites" : [
            {
               "name" : "truffle",
               "subdir" : True,
               "version" : "4190e63be1bacdc1238c4cd526725eaf498d6337",
               "urls" : [
                    {"url" : "https://github.com/graalvm/graal", "kind" : "git"},
                    {"url" : "https://curio.ssw.jku.at/nexus/content/repositories/snapshots", "kind" : "binary"},
                ]
            },

        ],
   },

  "repositories" : {
    "snapshots" : {
        "url" : "https://curio.ssw.jku.at/nexus/content/repositories/snapshots",
        "licenses" : ["GPLv2"]
    }
  },

  "licenses" : {
    "GPLv2" : {
      "name" : "GNU General Public License, version 2",
      "url" : "http://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html"
    },
  },

  "defaultLicense" : "GPLv2",

  # libraries that we depend on
  # N.B. The first four with a "path" attribute must be located
  # relative to the suite root and not the mx cache because they are
  # explicitly referenced in the Parser annotation processor.
  "libraries" : {
    "GNUR" : {
        "path" : "libdownloads/R-3.3.2.tar.gz",
        "urls" : ["http://cran.rstudio.com/src/base/R-3/R-3.3.2.tar.gz"],
        "sha1" : "0e39e9c2d28fe6bab9c55ca23e08ba8727fd2fca",
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

    "ANTLR-C-3.5" : {
      "path" : "libdownloads/antlr-complete-3.5.1.jar",
      "urls" : ["http://central.maven.org/maven2/org/antlr/antlr-complete/3.5.1/antlr-complete-3.5.1.jar"],
      "sha1" : "ebb4b995fd67a9b291ea5b19379509160f56e154",
    },

    "XZ-1.5" : {
      "path" : "libdownloads/xz-1.5.jar",
      "urls" : ["http://central.maven.org/maven2/org/tukaani/xz/1.5/xz-1.5.jar"],
      "sha1" : "9c64274b7dbb65288237216e3fae7877fd3f2bee",
    },

  },

  "projects" : {
    "com.oracle.truffle.r.parser.processor" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
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
        "com.oracle.truffle.r.runtime",
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
      "native" : True,
      "sourceDirs" : [],
      "dependencies" : ["com.oracle.truffle.r.native"],
      "platformDependent" : True,
      "output" : "com.oracle.truffle.r.test.native",
      "results" :[
         "urand/lib/liburand.so",
       ],
      "workingSets" : "FastR",
    },

    "com.oracle.truffle.r.test.packages" : {
      "sourceDirs" : ["r"],
      "javaCompliance" : "1.8",
      "workingSets" : "FastR",
    },

    "com.oracle.truffle.r.engine" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.r.nodes.builtin",
        "com.oracle.truffle.r.parser",
        "truffle:JLINE",
        "truffle:TRUFFLE_DEBUG",
        "truffle:TRUFFLE_NFI",
      ],
     "generatedDependencies" : [
        "com.oracle.truffle.r.parser",
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
        "truffle:TRUFFLE_DEBUG",
        "XZ-1.5",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "1.8",
      "annotationProcessors" : [
          "truffle:TRUFFLE_DSL_PROCESSOR",
      ],
      "workingSets" : "Truffle,FastR",
      "jacoco" : "include",
    },

    "com.oracle.truffle.r.ffi.impl" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
         "com.oracle.truffle.r.ffi.processor",
         "com.oracle.truffle.r.nodes"
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "1.8",
      "annotationProcessors" : [
          "truffle:TRUFFLE_DSL_PROCESSOR",
          "R_FFI_PROCESSOR",
      ],
      "workingSets" : "Truffle,FastR",
      "jacoco" : "include",
    },

    "com.oracle.truffle.r.ffi.processor" : {
      "sourceDirs" : ["src"],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "1.8",
      "workingSets" : "FastR",
    },

    "com.oracle.truffle.r.native" : {
      "sourceDirs" : [],
#      "class" : "FastRNativeProject",
      "dependencies" : [
        "GNUR",
        "GNU_ICONV",
        "truffle:TRUFFLE_NFI_NATIVE",
      ],
      "native" : "true",
      "workingSets" : "FastR",
      "buildEnv" : {
        "NFI_INCLUDES" : "-I<path:truffle:TRUFFLE_NFI_NATIVE>/include",
      },
    },

    "com.oracle.truffle.r.library" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.r.ffi.impl",
      ],
      "annotationProcessors" : [
          "truffle:TRUFFLE_DSL_PROCESSOR",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "1.8",
      "workingSets" : "FastR",
      "jacoco" : "include",

    },

    "com.oracle.truffle.r.release" : {
      "sourceDirs" : ["src"],
      "dependencies" : ["com.oracle.truffle.r.native.recommended"],
      "class" : "FastRReleaseProject",
      "output" : "com.oracle.truffle.r.release"
    },

    "com.oracle.truffle.r.native.recommended" : {
      "dependencies" : [
        "com.oracle.truffle.r.native",
        "com.oracle.truffle.r.engine",
        "com.oracle.truffle.r.ffi.impl"
      ],
      "class" : "FastRNativeRecommendedProject",
      "native" : "true",
      "workingSets" : "FastR",
    },

  },

  "distributions" : {
    "TRUFFLE_R_PARSER_PROCESSOR" : {
      "description" : "internal support for generating the R parser",
      "dependencies" : ["com.oracle.truffle.r.parser.processor"],
      "exclude" : [
        "ANTLR-3.5",
        "ANTLR-C-3.5",
       ],
       "maven" : "False",
    },

    "R_FFI_PROCESSOR" : {
      "description" : "internal support for generating FFI classes",
      "dependencies" : ["com.oracle.truffle.r.ffi.processor"],
      "maven" : "False",
    },

    "FASTR" : {
      "description" : "class files for compiling against FastR in a separate suite",
      "dependencies" : ["com.oracle.truffle.r.engine", "com.oracle.truffle.r.ffi.impl"],
      "mainClass" : "com.oracle.truffle.r.engine.shell.RCommand",
      "exclude" : [
        "truffle:JLINE",
        "ANTLR-C-3.5",
        "ANTLR-3.5",
        "GNUR",
        "GNU_ICONV",
        "XZ-1.5",
      ],
      "distDependencies" : [
        "truffle:TRUFFLE_API",
        "truffle:TRUFFLE_DEBUG",
        "truffle:TRUFFLE_NFI",
        "truffle:TRUFFLE_NFI_NATIVE",
        "TRUFFLE_R_PARSER_PROCESSOR",
      ],
    },

    "FASTR_UNIT_TESTS" : {
      "description" : "unit tests",
      "dependencies" : [
        "com.oracle.truffle.r.test",
        "com.oracle.truffle.r.nodes.test"
       ],
      "exclude": ["mx:HAMCREST", "mx:JUNIT", "mx:JMH"],
      "distDependencies" : [
        "FASTR",
        "truffle:TRUFFLE_API",
        "truffle:TRUFFLE_DEBUG",
        "TRUFFLE_R_PARSER_PROCESSOR",
        "truffle:TRUFFLE_TCK",
      ],


    },

    "FASTR_UNIT_TESTS_NATIVE" : {
      "description" : "unit tests support (from test.native project)",
       "native" : True,
       "platformDependent" : True,
      "dependencies" : [
        "com.oracle.truffle.r.test.native",
     ],
    },

    "FASTR_RELEASE": {
      "description" : "a binary release of FastR",
      "dependencies" : ["com.oracle.truffle.r.release"],
       "os_arch" : {
         "linux" : {
          "amd64" : {
            "path" : "mxbuild/dists/linux/amd64/fastr-release.jar",
          },
          "sparcv9" : {
            "path" : "mxbuild/dists/linux/sparcv9/fastr-release.jar",
          },
        },
        "darwin" : {
          "amd64" : {
            "path" : "mxbuild/dists/darwin/amd64/fastr-release.jar",
          },
        },
        "solaris" : {
          "sparcv9" : {
            "path" : "mxbuild/dists/solaris/sparcv9/fastr-release.jar",
          },
        },
      },
    },
  },
}

