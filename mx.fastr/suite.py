suite = {
  "mxversion" : "6.0.1",
  "name" : "fastr",
  "versionConflictResolution" : "latest",
  "version": "24.1.0",
  "imports" : {
    "suites" : [
            {
               "name" : "truffle",
               "subdir" : True,
               # The version must be the same as the version of Sulong
               # TRUFFLE REVISION (note: this is a marker for script that can update this)
               "version" : "8cde43a017d6eb5af9bdfba7913182e877ef7e51",
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
               "version" : "8cde43a017d6eb5af9bdfba7913182e877ef7e51",
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
    # rpart 4.1-15
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
      "urls" : ["https://cran.r-project.org/src/contrib/Archive/cluster/cluster_{version}.tar.gz"],
      "digest" : "sha512:54f4e4768106035ba0566254393bc392af66cdf9309d117a57d18ee29b93207740d38eac2a4c4db761db65a7300abdbcd2a5915a115d5b81e7f523dd7d583301",
      "resource" : "true"
    },

    # A recommended package "codetools" with a fixed version rather than taken from GNU-R.
    "CODETOOLS" : {
      "path" : "libdownloads/codetools.tar.gz",
      "ext" : ".tar.gz",
      "version" : "0.2-18",
      "urls" : ["https://cran.r-project.org/src/contrib/Archive/codetools/codetools_{version}.tar.gz"],
      "sha1" : "fa0fe4d67316ff49776e5bef1ba56c9334633e71",
      "resource" : "true"
    },

    "MATRIX" : {
      "path" : "libdownloads/matrix.tar.gz",
      "ext" : ".tar.gz",
      "version" : "1.4-0",
      "urls" : ["https://cran.r-project.org/src/contrib/Archive/Matrix/Matrix_{version}.tar.gz"],
      "sha1" : "2745b86754e1becfae6cbea5e4715f87d3fe8464",
      "resource" : "true"
    },

    "BATIK-SVGGEN-1.14" : {
      "digest" : "sha512:40c87d3d25182ecd2490890100f70a4cd53523fda59438d2df9229f55d04f9c0f45757142a214b37b701290e23468df4061d00daa5cbcb533f95cca36cec4343",
      "maven" : {
        "groupId" : "org.apache.xmlgraphics",
        "artifactId" : "batik-svggen",
        "version" : "1.14",
      },
    },

    "BATIK-DOM-1.14" : {
      "digest" : "sha512:923ecb2e19576180dd212681416414f6d546d22df8af34ce88e493f6f45b2c4267d812531bba7304df36690c334f87db1eba63c08314f131114fb2ae0ad7aa16",
      "maven" : {
        "groupId" : "org.apache.xmlgraphics",
        "artifactId" : "batik-dom",
        "version" : "1.14",
      },
    },

    "BATIK-AWT-UTIL-1.14" : {
      "digest" : "sha512:ca51d78991c2e10e602f7223a61507ea2dcdc7e62f266f803aecd63de6f7819e565ce9ad36adbe54ce733962a60b2708b3cdba0c583cacb9cd38d3db7762f32f",
      "maven" : {
        "groupId" : "org.apache.xmlgraphics",
        "artifactId" : "batik-awt-util",
        "version" : "1.14",
      },
    },

    "BATIK-UTIL-1.14" : {
      "digest" : "sha512:6dca37c52f954c677ca003e6f7c7c43a553d73883d936a71497fb3484d277def9a13bda35e857ff61ff724c8dfb257e636a9d11d9163f1cd313d86394fe6d176",
      "maven" : {
        "groupId" : "org.apache.xmlgraphics",
        "artifactId" : "batik-util",
        "version" : "1.14",
      },
    },

    "BATIK-CONSTANTS-1.14" : {
      "digest" : "sha512:b6e859e4cdb0c6f338955d5aacf26b08a24dea4ee043258a2e1ea84690f5c4fa219c1f5cabb7bc40499144a0c4fdcd964479c0afe7e09eac471490b67cb40bc8",
      "maven" : {
        "groupId" : "org.apache.xmlgraphics",
        "artifactId" : "batik-constants",
        "version" : "1.14",
      },
    },

    "BATIK-I18N-1.14" : {
      "digest" : "sha512:1631b8ccf27ea201c595cdf11f78a6ab0bbafd89e1e2ca2c55a4a5dd8ea200d09128fd5464a1c03ff9813a431407d6e21dc1c0351dfd1812d9af63bb24a8c111",
      "maven" : {
        "groupId" : "org.apache.xmlgraphics",
        "artifactId" : "batik-i18n",
        "version" : "1.14",
      },
    },

    "BATIK-EXT-1.14" : {
      "digest" : "sha512:ea86979d559ac16fb09bd49984ffc2f13ebdbf5e4bd054d3620b01e923afc0c189fdb47485a440507c456a752b228ffa1a7413169f2faaf9f3fa80946ca5ae44",
      "maven" : {
        "groupId" : "org.apache.xmlgraphics",
        "artifactId" : "batik-ext",
        "version" : "1.14",
      },
    },

    "BATIK-XML-1.14" : {
      "digest" : "sha512:0fa51a33cae4d36f18e5709647d96204eb0ca2327a7d9434f5210c1c6730006094656c067f961d508cc1780fc565b59df1546fab39abf58311cfbe4248eb092b",
      "maven" : {
        "groupId" : "org.apache.xmlgraphics",
        "artifactId" : "batik-xml",
        "version" : "1.14",
      },
    },
  },

  "projects" : {

    "com.oracle.truffle.r.common" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "sdk:GRAAL_SDK",
      ],
      "requires" : [
        "java.management",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "17+",
      "workingSets" : "Truffle,FastR",
      "jacoco" : "include",
    },

    "com.oracle.truffle.r.parser" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.r.runtime",
            "truffle:ANTLR4",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "17+",
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
      "javaCompliance" : "17+",
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
      "forceJavac": "true",
    },

    "com.oracle.truffle.r.nodes.builtin" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.r.library",
        "sulong:SULONG_API",
      ],
      "requires" : [
        "java.desktop",
        "java.logging",
        "jdk.unsupported", # sun.misc.Unsafe
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "17+",
      "annotationProcessors" : [
        "truffle:TRUFFLE_DSL_PROCESSOR",
      ],
      "spotbugsIgnoresGenerated" : True,
      "workingSets" : "Truffle,FastR",
      "jacoco" : "include",
      "forceJavac": "true",
    },

    "com.oracle.truffle.r.nodes.test" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.r.test",
      ],
      "testProject": True,
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "17+",
      "workingSets" : "Truffle,FastR,Test",
      "jacoco" : "include",
      "spotbugsIgnoresGenerated" : True,
    },

    "com.oracle.truffle.r.test" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "mx:JUNIT",
        "truffle:TRUFFLE_TCK",
        "com.oracle.truffle.r.launcher",
        "com.oracle.truffle.r.engine",
      ],
      "requires" : [
        "java.logging",
      ],
      "annotationProcessors" : [
          "truffle:TRUFFLE_DSL_PROCESSOR",
      ],
      "testProject": True,
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "17+",
      "workingSets" : "Truffle,FastR,Test",
      "jacoco" : "include",
      "spotbugsIgnoresGenerated" : True,
    },

    "com.oracle.truffle.r.test.integration" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "mx:JUNIT",
        "sdk:GRAAL_SDK",
      ],
      "requires" : [
        "java.logging",
      ],
      "testProject": True,
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "17+",
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
      "javaCompliance" : "17+",
      "workingSets" : "FastR",
    },

    "com.oracle.truffle.r.test.packages.analyzer" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "mx:JUNIT"
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "17+",
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
      "javaCompliance" : "17+",
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
        "com.oracle.truffle.r.common",
        "truffle:TRUFFLE_API",
        "sulong:SULONG_API",
        "truffle:TRUFFLE_XZ",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "checkstyleVersion": "8.8",
      "javaCompliance" : "17+",
      "annotationProcessors" : [
          "truffle:TRUFFLE_DSL_PROCESSOR",
      ],
      "workingSets" : "Truffle,FastR",
      "jacoco" : "include",
      "spotbugsIgnoresGenerated" : True,
      "requires" : [
        "java.management",
        "java.desktop",
        "java.logging",
        "jdk.unsupported" # sun.misc.Unsafe
      ],
    },

    "com.oracle.truffle.r.launcher" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
        "com.oracle.truffle.r.common",
        "sdk:GRAAL_SDK",
        "sdk:LAUNCHER_COMMON",
        "sdk:JLINE3",
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "17+",
      "workingSets" : "Truffle,FastR",
      "jacoco" : "include",
    },

    "com.oracle.truffle.r.ffi.impl" : {
      "sourceDirs" : ["src"],
      "dependencies" : [
         "com.oracle.truffle.r.ffi.processor",
         "com.oracle.truffle.r.nodes",
         "org.rosuda.javaGD",
         "truffle:TRUFFLE_NFI",
         "BATIK-SVGGEN-1.14",
         "BATIK-DOM-1.14",
         "BATIK-AWT-UTIL-1.14",
         "BATIK-UTIL-1.14",
         "BATIK-CONSTANTS-1.14",
         "BATIK-I18N-1.14",
         "BATIK-EXT-1.14",
         "BATIK-XML-1.14",
      ],
      "requires" : [
        "java.desktop",
        "jdk.unsupported" # sun.misc.Unsafe
      ],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "17+",
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
      "javaCompliance" : "17+",
      "workingSets" : "FastR",
    },

    "com.oracle.truffle.r.ffi.processor" : {
      "sourceDirs" : ["src"],
      "checkstyle" : "com.oracle.truffle.r.runtime",
      "javaCompliance" : "17+",
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
      "javaCompliance" : "17+",
      "workingSets" : "FastR",
      "jacoco" : "include",
      "spotbugsIgnoresGenerated" : True,
    },

    "org.rosuda.javaGD" : {
      "sourceDirs" : ["src"],
      "dependencies" : [],
      "requires": ["java.desktop"],
      "checkstyle" : "org.rosuda.javaGD",
      "javaCompliance" : "17+",
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
        "CODETOOLS",
        "MATRIX",
        "com.oracle.truffle.r.native",
        "com.oracle.truffle.r.engine",
        "com.oracle.truffle.r.ffi.impl",
        "com.oracle.truffle.r.common",
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
      "javaCompliance" : "17+",
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

    "FASTR_COMMON" : {
      "moduleInfo" : {
          "name" : "org.graalvm.r.common",
          "exports" : [
              "com.oracle.truffle.r.common to org.graalvm.r",
            "com.oracle.truffle.r.common to org.graalvm.r.launcher",
          ],
      },
      "useModulePath": True,
      "description" : "common utilities shared by fastr and fastr-launcher",
      "dependencies" : ["com.oracle.truffle.r.common"],
      "distDependencies" : [
        "sdk:GRAAL_SDK",
      ],
      # Uncomment to publish Maven artifacts
      # Use: sed 's/\([ ]*\)#\([^#]*\)#Maven/\1\2/g'
      # "noMavenJavadoc": True,         #Maven
      # "maven": {                      #Maven
      #   "artifactId": "r-common",     #Maven
      #   "groupId": "org.graalvm.r",   #Maven
      #   "tag": ["default", "public"], #Maven
      # },                              #Maven
      # "license": ["GPLv3"],           #Maven
    },

    "FASTR_LAUNCHER" : {
      "moduleInfo" : {
        "name" : "org.graalvm.r.launcher",
        "exports" : [
          "com.oracle.truffle.r.launcher to org.graalvm.launcher"
        ]
      },
      "useModulePath": True,
      "description" : "launcher for the GraalVM (at the moment used only when native image is installed)",
      "dependencies" : ["com.oracle.truffle.r.launcher"],
      "distDependencies" : [
        "FASTR_COMMON",
        "sdk:GRAAL_SDK",
        "sdk:LAUNCHER_COMMON",
        "sdk:JLINE3",
      ],
      # Uncomment to publish Maven artifacts
      # "noMavenJavadoc": True,         #Maven
      # "maven": {                      #Maven
      #   "artifactId": "r-launcher",   #Maven
      #   "groupId": "org.graalvm.r",   #Maven
      #   "tag": ["default", "public"], #Maven
    # },                                #Maven
      # "license": ["GPLv3"],           #Maven
    },

    "FASTR" : {
      "moduleInfo" : {
          "name" : "org.graalvm.r",
          "requires": [
            "java.base",
            "java.logging",
            "java.management",
            "jdk.unsupported",
            "jdk.xml.dom",
            "java.desktop",
            "batik.svggen",
            "batik.dom",
            "batik.awt.util",
            "batik.util",
            "batik.constants",
            "batik.i18n",
            "batik.ext",
            "batik.xml",
          ],
      },
      "useModulePath": True,
      "description" : "class files for compiling against FastR in a separate suite",
      "dependencies" : [
        "com.oracle.truffle.r.engine",
        "com.oracle.truffle.r.ffi.impl"
      ],
      "mainClass" : "com.oracle.truffle.r.launcher.RCommand",
      "exclude" : [
        "truffle:ANTLR4",
        "GNUR",
        "BATIK-SVGGEN-1.14",
        "BATIK-DOM-1.14",
        "BATIK-AWT-UTIL-1.14",
        "BATIK-UTIL-1.14",
        "BATIK-CONSTANTS-1.14",
        "BATIK-I18N-1.14",
        "BATIK-EXT-1.14",
        "BATIK-XML-1.14",
      ],
      "distDependencies" : [
        "FASTR_COMMON",
        "truffle:TRUFFLE_API",
        "truffle:TRUFFLE_NFI",
        "truffle:TRUFFLE_NFI_LIBFFI",
        "truffle:TRUFFLE_XZ",
        "sulong:SULONG_API",
        "sdk:JLINE3",
      ],
      # Uncomment to publish Maven artifacts
      # "noMavenJavadoc": True,         #Maven
      # "maven": {                      #Maven
      #   "artifactId": "r-language",   #Maven
      #   "groupId": "org.graalvm.r",   #Maven
      #   "tag": ["default", "public"], #Maven
      # },                              #Maven
      # "license": ["GPLv3"],           #Maven
    },

    # Uncomment to publish Maven artifacts
    # "R_COMMUNITY": {                        #Maven
    #   "type": "pom",                        #Maven
    #   "runtimeDependencies": [              #Maven
    #     "FASTR",                            #Maven
    #     "FASTR_COMMON",                     #Maven
    #   ],                                    #Maven
    #   "description": "FastR engine.",       #Maven
    #   "maven": {                            #Maven
    #     "groupId": "org.graalvm.polyglot",  #Maven
    #     "artifactId": "r-community",        #Maven
    #     "tag": ["default", "public"],       #Maven
    #   },                                    #Maven
    #   "license": ["GPLv3"],                 #Maven
    # },                                      #Maven

    "FASTR_COMMUNITY" : {
      "type" : "pom",
      "runtimeDependencies" : [
        "fastr:FASTR_COMMON",
        "fastr:FASTR",
      ],
      "description" : "FastR support distribution for the GraalVM",
      "maven" : False,
    },

    "FASTR_UNIT_TESTS" : {
      "description" : "unit tests",
      "dependencies" : [
        "com.oracle.truffle.r.test",
        "com.oracle.truffle.r.test.integration",
        "com.oracle.truffle.r.nodes.test",
       ],
      "exclude": ["mx:HAMCREST", "mx:JUNIT"],
      "distDependencies" : [
        "FASTR",
        "FASTR_LAUNCHER",
        "sulong:SULONG_NATIVE",
        "truffle:TRUFFLE_API",
        "truffle:TRUFFLE_TCK",
      ],
      "unittestConfig": "fastr-tests",
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
        "FASTR",
      ],
      "maven" : False
    },

    # see mx_fastr_dists.mx_register_dynamic_suite_constituents for the definitions of some RFFI-dependent distributions
  },
}
