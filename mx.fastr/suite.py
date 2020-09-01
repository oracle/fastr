suite = {
  "mxversion" : "5.210.4",
  "name" : "fastr",
  "versionConflictResolution" : "latest",
  "imports" : {
    "suites" : [
            {
               "name" : "truffle",
               "subdir" : True,
               # The version must be the same as the version of Sulong
               # TRUFFLE REVISION (note: this is a marker for script that can update this)
               "version" : "c28201d846702acff38cae0910efc5a4069ea4e0",
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
               "version" : "c28201d846702acff38cae0910efc5a4069ea4e0",
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
        "path" : "libdownloads/R-3.6.1.tar.gz", # keep in sync with the GraalVM support distribution
        "urls" : ["https://cran.rstudio.com/src/base/R-3/R-3.6.1.tar.gz"],
        "sha1" : "0041c025561b5622e3dc88cdedf82334620cd3c2",
        "resource" : "true"
    },

    "F2C" : {
        "path" : "libdownloads/f2c/src.tgz",
        "urls" : ["https://netlib.sandia.gov/f2c/src.tgz"],
        "sha1" : "7cf7eaa87d4800f7abd54ef882211c82426de764",
        "resource" : "true"
    },

    "LIBF2C" : {
        "path" : "libdownloads/f2c/libf2c.zip",
        "urls" : ["https://netlib.sandia.gov/f2c/libf2c.zip"],
        "sha1" : "e39a00f425f8fc41dde434686080a94e94884f30",
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
    "BATIK-ALL-1.11" : {
      "sha1" : "5fbd444c4160b9017bf107622347a12624d2749d",
      "maven" : {
        "groupId" : "org.apache.xmlgraphics",
        "artifactId" : "batik-all",
        "version" : "1.11",
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
      "javaCompliance" : "8+",
      "spotbugsIgnoresGenerated" : True,
      "workingSets" : "Truffle,FastR",
      "jacoco" : "include",
    },

    "com.oracle.truffle.r.nodes" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.r.runtime",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "8+",
      "annotationProcessors" : [
          "truffle:TRUFFLE_DSL_PROCESSOR",
      ],
      "spotbugsIgnoresGenerated" : True,
      "workingSets" : "Truffle,FastR",
      "jacoco" : "include",
    },

    "com.oracle.truffle.r.nodes.builtin" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.r.library",
        "sulong:SULONG_API",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "8+",
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
      "javaCompliance" : "8+",
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
      "annotationProcessors" : [
          "truffle:TRUFFLE_DSL_PROCESSOR",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "8+",
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
      "javaCompliance" : "8+",
      "workingSets" : "FastR",
    },

    "com.oracle.truffle.r.test.packages.analyzer" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "mx:JUNIT"
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "8+",
      "workingSets" : "FastR",
    },

    "com.oracle.truffle.r.engine" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.r.nodes.builtin",
        "com.oracle.truffle.r.parser",
        "truffle:JLINE",
        "truffle:TRUFFLE_NFI",
      ],
     "generatedDependencies" : [
        "com.oracle.truffle.r.parser",
     ],
      "annotationProcessors" : [
          "truffle:TRUFFLE_DSL_PROCESSOR",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "8+",
      "workingSets" : "Truffle,FastR",
      "jacoco" : "include",
      "spotbugsIgnoresGenerated" : True,
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
      "javaCompliance" : "8+",
      "annotationProcessors" : [
          "truffle:TRUFFLE_DSL_PROCESSOR",
      ],
      "workingSets" : "Truffle,FastR",
      "jacoco" : "include",
      "spotbugsIgnoresGenerated" : True,
    },

    "com.oracle.truffle.r.launcher" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "sdk:GRAAL_SDK",
        "sdk:LAUNCHER_COMMON",
        "truffle:JLINE",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "8+",
      "annotationProcessors" : [
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
         'BATIK-ALL-1.11',
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "8+",
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
      "javaCompliance" : "8+",
      "workingSets" : "FastR",
    },

    "com.oracle.truffle.r.ffi.processor" : {
      "sourceDirs" : ["src"],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "8+",
      "workingSets" : "FastR",
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
      "annotationProcessors" : [
          "truffle:TRUFFLE_DSL_PROCESSOR",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "8+",
      "workingSets" : "FastR",
      "jacoco" : "include",
      "spotbugsIgnoresGenerated" : True,
    },

    "com.oracle.truffle.r.library.fastrGrid.server" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.r.library",
      ],
      "annotationProcessors" : [
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "8+",
      "workingSets" : "FastR",
      "jacoco" : "include",

    },

    "org.rosuda.javaGD" : {
      "sourceDirs" : ["src"],
      "dependencies" : [],
      "checkstyle" : "org.rosuda.javaGD",
      "javaCompliance" : "8+",
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
      "javaCompliance" : "8+",
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
        "truffle:JLINE",
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

    "GRID_DEVICE_REMOTE_SERVER" : {
      "description" : "remote server for grid device",
      "dependencies" : [
        "com.oracle.truffle.r.library.fastrGrid.server",
      ],
      "mainClass" : "com.oracle.truffle.r.library.fastrGrid.server.RemoteDeviceServer",
      "exclude" : [
        "truffle:JLINE",
        "truffle:ANTLR4",
        "GNUR",
        "XZ-1.8",
      ],
      "distDependencies" : [
        "truffle:TRUFFLE_API",
        "truffle:TRUFFLE_NFI",
        "truffle:TRUFFLE_NFI_NATIVE",
        "sulong:SULONG_API",
        "fastr:FASTR",
      ],
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
