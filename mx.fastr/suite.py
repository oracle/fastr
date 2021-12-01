suite = {
  "mxversion" : "5.316.13",
  "name" : "fastr",
  "versionConflictResolution" : "latest",
  "imports" : {
    "suites" : [
            {
               "name" : "truffle",
               "subdir" : True,
               # The version must be the same as the version of Sulong
               # TRUFFLE REVISION (note: this is a marker for script that can update this)
               "version" : "92565c9ea2f46f8d719c79ac9846984c8fc77c65",
               "urls" : [
                    {"url" : "https://github.com/graalvm/graal", "kind" : "git"},
                    {"url" : "https://curio.ssw.jku.at/nexus/content/repositories/snapshots", "kind" : "binary"},
                ]
            },
            {
               "name" : "sulong",
               "subdir" : True,
               # The version must be the same as the version of Truffle
               # TRUFFLE REVISION (note: this is a marker for script that can update this)
               "version" : "92565c9ea2f46f8d719c79ac9846984c8fc77c65",
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
      "licenses" : ["GPLv3"]
    }
  },

  "licenses" : {
    "GPLv3" : {
      "name" : "GNU General Public License, version 3",
      "url" : "https://www.gnu.org/licenses/gpl-3.0.html"
    },
  },

  "defaultLicense" : "GPLv3",

  # libraries that we depend on
  "libraries" : {
    "GNUR" : {
      "path" : "libdownloads/R-4.0.3.tar.gz", # keep in sync with the GraalVM support distribution
      "urls" : ["https://cran.rstudio.com/src/base/R-4/R-4.0.3.tar.gz"],
      "sha1" : "5daba2d63e07a9f39d9b69b68f0642d71213ec5c",
      "resource" : "true"
    },

    "F2C" : {
      "path" : "libdownloads/f2c/src.tgz",
      "urls" : ["https://lafo.ssw.uni-linz.ac.at/pub/graal-external-deps/f2c/20191129/src.tgz"],
      "sha1" : "8a26107bf9f82a2dcfa597f15549a412be75e0ee",
      "resource" : "true"
    },

    "LIBF2C" : {
      "path" : "libdownloads/f2c/libf2c.zip",
      "urls" : ["https://lafo.ssw.uni-linz.ac.at/pub/graal-external-deps/f2c/20191129/libf2c.zip"],
      "sha1" : "e39a00f425f8fc41dde434686080a94e94884f30",
      "resource" : "true"
    },

    # A recommended package "rpart" with a fixed version rather than taken from GNU-R.
    "RPART" : {
      "path" : "libdownloads/rpart.tar.gz",
      "ext" : ".tar.gz",
      "version" : "4020bb4ee8fd6739bd97e8c39931fa7e3901300c",
      "urls" : ["https://api.github.com/repos/bethatkinson/rpart/tarball/{version}"],
      "sha1" : "ec76dbd51acad10bed843a0005ba5fdcf5c7a35d",
      "resource" : "true"
    },

    # A recommended package "cluster" with a fixed version rather than taken from GNU-R.
    "CLUSTER" : {
      "path" : "libdownloads/cluster.tar.gz",
      "ext" : ".tar.gz",
      "version" : "2.1.2",
      "urls" : ["https://cran.r-project.org/src/contrib/cluster_{version}.tar.gz"],
      "sha1" : "47763fa44d11e0f2c2feafade3e331c05eda30d1",
      "resource" : "true"
    },

    "XZ-1.8" : {
      "sha1" : "c4f7d054303948eb6a4066194253886c8af07128",
      "maven" : {
        "groupId" : "org.tukaani",
        "artifactId" : "xz",
        "version" : "1.8",
      },
    },
    "BATIK-ALL-1.14" : {
      "sha1" : "a8d228e4ae2c21efb833fdfcdfe5446fa672974a",
      "maven" : {
        "groupId" : "org.apache.xmlgraphics",
        "artifactId" : "batik-all",
        "version" : "1.14",
      },
    },
  },

  "projects" : {
    "com.oracle.truffle.r.parser" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.r.runtime",
            "truffle:ANTLR4",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "11+",
      "spotbugsIgnoresGenerated" : True,
      "workingSets" : "Truffle,FastR",
      "jacoco" : "include",
      "requires" : [
        "java.logging",
      ],
    },

    "com.oracle.truffle.r.nodes" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.r.runtime",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "11+",
      "annotationProcessors" : [
          "truffle:TRUFFLE_DSL_PROCESSOR",
      ],
      "spotbugsIgnoresGenerated" : True,
      "workingSets" : "Truffle,FastR",
      "jacoco" : "include",
      "requires" : [
        "java.logging",
        "jdk.unsupported", # sun.misc.Unsafe
      ],
    },

    "com.oracle.truffle.r.nodes.builtin" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.r.library",
        "sulong:SULONG_API",
      ],
      "requires" : [
        "java.logging",
        "jdk.unsupported", # sun.misc.Unsafe
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "11+",
      "annotationProcessors" : [
        "truffle:TRUFFLE_DSL_PROCESSOR",
      ],
      "spotbugsIgnoresGenerated" : True,
      "workingSets" : "Truffle,FastR",
      "jacoco" : "include",
    },

    "com.oracle.truffle.r.nodes.test" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.r.test",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "11+",
      "workingSets" : "Truffle,FastR,Test",
      "jacoco" : "include",
      "spotbugsIgnoresGenerated" : True,
    },

    "com.oracle.truffle.r.test" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "mx:JUNIT",
        "truffle:TRUFFLE_TCK",
        "com.oracle.truffle.r.engine",
      ],
      "requires" : [
        "java.logging",
      ],
      "annotationProcessors" : [
          "truffle:TRUFFLE_DSL_PROCESSOR",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "11+",
      "workingSets" : "Truffle,FastR,Test",
      "jacoco" : "include",
      "spotbugsIgnoresGenerated" : True,
    },

    "com.oracle.truffle.r.test.native" : {
      "native" : True,
      "sourceDirs" : [],
      "dependencies" : ["com.oracle.truffle.r.native"],
      "platformDependent" : True,
      "output" : "com.oracle.truffle.r.test.native",
      "buildEnv" : {
        "LABS_LLVM_CC": "<toolchainGetToolPath:native,CC>",
        "LABS_LLVM_CXX": "<toolchainGetToolPath:native,CXX>",
      },
      "results" :[
         "urand/lib/liburand.so",
       ],
      "workingSets" : "FastR",
      "spotbugsIgnoresGenerated" : True,
    },

    "com.oracle.truffle.r.test.packages" : {
      "sourceDirs" : ["r"],
      "javaCompliance" : "11+",
      "workingSets" : "FastR",
    },

    "com.oracle.truffle.r.test.packages.analyzer" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "mx:JUNIT"
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "11+",
      "workingSets" : "FastR",
      "requires" : [
        "java.logging"
      ],
    },

    "com.oracle.truffle.r.engine" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.r.nodes.builtin",
        "com.oracle.truffle.r.parser",
        "sdk:JLINE3",
        "truffle:TRUFFLE_NFI",
      ],
     "generatedDependencies" : [
        "com.oracle.truffle.r.parser",
     ],
      "annotationProcessors" : [
          "truffle:TRUFFLE_DSL_PROCESSOR",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "11+",
      "workingSets" : "Truffle,FastR",
      "jacoco" : "include",
      "spotbugsIgnoresGenerated" : True,
      "requires" : [
        "java.logging",
      ],
    },

    "com.oracle.truffle.r.runtime" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.r.launcher",
        "truffle:TRUFFLE_API",
        "sulong:SULONG_API",
        "XZ-1.8",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "checkstyleVersion": "8.8",
      "javaCompliance" : "11+",
      "annotationProcessors" : [
          "truffle:TRUFFLE_DSL_PROCESSOR",
      ],
      "workingSets" : "Truffle,FastR",
      "jacoco" : "include",
      "spotbugsIgnoresGenerated" : True,
      "requires" : [
        "java.management",
        "java.logging",
        "jdk.unsupported" # sun.misc.Unsafe
      ],
    },

    "com.oracle.truffle.r.launcher" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "sdk:GRAAL_SDK",
        "sdk:LAUNCHER_COMMON",
        "sdk:JLINE3",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "11+",
      "requires" : [
        "java.management"
      ],
      "workingSets" : "Truffle,FastR",
      "jacoco" : "include",
    },

    "com.oracle.truffle.r.ffi.impl" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
         "com.oracle.truffle.r.ffi.processor",
         "com.oracle.truffle.r.nodes",
         "org.rosuda.javaGD",
         'BATIK-ALL-1.14',
      ],
      "requires" : [
        "java.desktop",
        "jdk.unsupported" # sun.misc.Unsafe
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "11+",
      "annotationProcessors" : [
          "truffle:TRUFFLE_DSL_PROCESSOR",
          "R_FFI_PROCESSOR",
      ],
      "workingSets" : "Truffle,FastR",
      "jacoco" : "include",
      "spotbugsIgnoresGenerated" : True,
    },

    "com.oracle.truffle.r.ffi.codegen" : {
      "sourceDirs" : ["src"],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "dependencies" : [
        "com.oracle.truffle.r.ffi.impl"
      ],
      "javaCompliance" : "11+",
      "workingSets" : "FastR",
    },

    "com.oracle.truffle.r.ffi.processor" : {
      "sourceDirs" : ["src"],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "11+",
      "workingSets" : "FastR",
      "requires" : [
        "java.compiler"
      ],
    },

    "com.oracle.truffle.r.native" : {
      "sourceDirs" : [],
      "dependencies" : [
        "GNUR",
        "F2C",
        "LIBF2C",
        "truffle:TRUFFLE_NFI_NATIVE",
        "sulong:SULONG_BOOTSTRAP_TOOLCHAIN",
        "sulong:SULONG_HOME",
        "sulong:SULONG_LEGACY",
      ],
      "native" : True,
      "single_job" : True,
      "workingSets" : "FastR",
      "buildEnv" : {
        "NFI_INCLUDES" : "-I<path:truffle:TRUFFLE_NFI_NATIVE>/include",
        "LLVM_INCLUDES" : "-I<path:sulong:SULONG_LEGACY>/include -I<path:sulong:SULONG_HOME>/include",
        "LLVM_LIBS_DIR" : "<path:sulong:SULONG_HOME>",
        # If FASTR_RFFI=='llvm', then this is set as CC/CXX in c.o.t.r.native/Makefile
        "LABS_LLVM_CC": "<toolchainGetToolPath:native,CC>",
        "LABS_LLVM_CXX": "<toolchainGetToolPath:native,CXX>",
        "GRAALVM_VERSION": "<graalvm_version>",
      },
    },

    "com.oracle.truffle.r.library" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.r.ffi.impl",
      ],
      "requires" : [
        "java.desktop",
        "jdk.unsupported", # sun.misc.Unsafe
      ],
      "annotationProcessors" : [
          "truffle:TRUFFLE_DSL_PROCESSOR",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "11+",
      "workingSets" : "FastR",
      "jacoco" : "include",
      "spotbugsIgnoresGenerated" : True,
    },

    "org.rosuda.javaGD" : {
      "sourceDirs" : ["src"],
      "dependencies" : [],
      "requires": ["java.desktop"],
      "checkstyle" : "org.rosuda.javaGD",
      "javaCompliance" : "11+",
      "workingSets" : "FastR",
      "jacoco" : "include",
    },

    "com.oracle.truffle.r.release" : {
      "sourceDirs" : ["src"],
      "buildDependencies" : ["com.oracle.truffle.r.native.recommended"],
      "class" : "FastRReleaseProject",
      "output" : "com.oracle.truffle.r.release",
    },

    "com.oracle.truffle.r.native.recommended" : {
      "dependencies" : [
        "RPART",
        "CLUSTER",
        "com.oracle.truffle.r.native",
        "com.oracle.truffle.r.engine",
        "com.oracle.truffle.r.ffi.impl",
        "com.oracle.truffle.r.launcher"
      ],
      "max_jobs" : "8",
      "native" : True,
      "vpath": True,
      "workingSets" : "FastR",
      "buildDependencies" : ["FASTR"],
    },

    "com.oracle.truffle.r.test.tck" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "mx:JUNIT",
        "sdk:POLYGLOT_TCK",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "11+",
      "workingSets" : "FastR,Test",
      "spotbugsIgnoresGenerated" : True,
    },
  },

  "distributions" : {
    "R_FFI_PROCESSOR" : {
      "description" : "internal support for generating FFI classes",
      "dependencies" : ["com.oracle.truffle.r.ffi.processor"],
      "maven" : "False",
      # FASTR and R_FFI_PROCESSOR share the actual annotations
      # This could be refactored so that we have one project with just the annotations and FASTR would depend only on that
      "overlaps": ["FASTR"],
    },

    "FASTR_LAUNCHER" : {
      "description" : "launcher for the GraalVM (at the moment used only when native image is installed)",
      "dependencies" : ["com.oracle.truffle.r.launcher"],
      "distDependencies" : [
        "sdk:GRAAL_SDK"
      ],
      # FASTR and FASTR_LAUNCHER share one common helper class RCmdOptions
      # This could be refactored in the future
      "overlaps": ["FASTR"],
    },

    "FASTR" : {
      "description" : "class files for compiling against FastR in a separate suite",
      "dependencies" : [
        "com.oracle.truffle.r.engine",
        "com.oracle.truffle.r.launcher",
        "com.oracle.truffle.r.ffi.impl"
      ],
      "mainClass" : "com.oracle.truffle.r.launcher.RCommand",
      "exclude" : [
        "sdk:JLINE3",
        "truffle:ANTLR4",
        "GNUR",
        "XZ-1.8",
      ],
      "distDependencies" : [
        "truffle:TRUFFLE_API",
        "truffle:TRUFFLE_NFI",
        "truffle:TRUFFLE_NFI_NATIVE",
        "sulong:SULONG_API",
      ],
      # TODO: is this intentional that we embed things from LAUNCHER_COMMON?
      "overlaps": ["sdk:LAUNCHER_COMMON"],
    },

    "FASTR_UNIT_TESTS" : {
      "description" : "unit tests",
      "dependencies" : [
        "com.oracle.truffle.r.test",
        "com.oracle.truffle.r.nodes.test"
       ],
      "exclude": ["mx:HAMCREST", "mx:JUNIT"],
      "distDependencies" : [
        "FASTR",
        "truffle:TRUFFLE_API",
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

    "TRUFFLE_R_TCK" : {
      "description" : "TCK tests provider",
      "dependencies" : [
        "com.oracle.truffle.r.test.tck"
      ],
      "exclude" : [
        "mx:JUNIT",
      ],
      "distDependencies" : [
        "sdk:POLYGLOT_TCK",
      ],
      "maven" : False
    },

    # see mx_fastr_dists.mx_register_dynamic_suite_constituents for the definitions of some RFFI-dependent distributions
  },
}
