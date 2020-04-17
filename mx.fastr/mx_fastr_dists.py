#
# Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
#
# This code is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License version 3 only, as
# published by the Free Software Foundation.
#
# This code is distributed in the hope that it will be useful, but WITHOUT
# ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
# FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
# version 3 for more details (a copy is included in the LICENSE file that
# accompanied this code).
#
# You should have received a copy of the GNU General Public License version
# 3 along with this work; if not, write to the Free Software Foundation,
# Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
#
# Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
# or visit www.oracle.com if you need additional information or have any
# questions.
#
import mx
import mx_sdk
import os, string, shutil
from os.path import join, basename, isfile
import distutils
# for some reason the script fails without this import:
from distutils import dir_util # pylint: disable=unused-import,no-name-in-module

_fastr_suite = mx.suite('fastr')


class FastRProjectAdapter(mx.ArchivableProject):
    def __init__(self, suite, name, deps, workingSets, theLicense, **args):
        mx.ArchivableProject.__init__(self, suite, name, deps, workingSets, theLicense)
        self.dir = join(suite.dir, name)

    def output_dir(self):
        return self.dir

    def archive_prefix(self):
        return ""

    def _get_files(self, d, results, filterfun=None):
        for root, _, files in os.walk(join(self.dir, d)):
            for f in files:
                if not filterfun or filterfun(f):
                    results.append(join(root, f))

class FastRReleaseProject(FastRProjectAdapter):  # pylint: disable=too-many-ancestors
    '''
    Custom class for creating the FastR release project, which supports the
    FASTR_RELEASE distribution.
    '''
    def __init__(self, suite, name, deps, workingSets, theLicense, **args):
        FastRProjectAdapter.__init__(self, suite, name, deps, workingSets, theLicense)

    def getResults(self):
        results = []
        for rdir in ['bin', 'include', 'lib', 'library', 'etc', 'share', 'doc']:
            self._get_files(rdir, results)
        results.append(join(self.dir, 'LICENSE'))
        results.append(join(self.dir, 'COPYRIGHT'))
        results.append(join(self.dir, 'README.md'))
        return results

    def getBuildTask(self, args):
        return ReleaseBuildTask(self, args)

class ReleaseBuildTask(mx.NativeBuildTask):
    def __init__(self, project, args):
        mx.NativeBuildTask.__init__(self, args, project)

    def _template(self, source, target, dictionary):
        class LauncherTemplate(string.Template):
            delimiter = '%%'
        with open(target, "w") as targetFile:
            targetFile.write(LauncherTemplate(open(source).read()).substitute(dictionary))

    def build(self):
        # copy the release directories
        output_dir = self.subject.dir
        fastr_dir = _fastr_suite.dir
        for d in ['bin', 'include', 'library', 'etc', 'share', 'doc']:
            target_dir = join(output_dir, d)
            distutils.dir_util.copy_tree(join(fastr_dir, d), target_dir, update=True)

        lib_fastr_dir = join(fastr_dir, 'lib')
        lib_output_dir = join(output_dir, 'lib')
        if os.path.exists(lib_output_dir):
            shutil.rmtree(lib_output_dir)
        os.mkdir(lib_output_dir)
        for f in os.listdir(lib_fastr_dir):
            source_file = join(lib_fastr_dir, f)
            target_file = join(lib_output_dir, f)
            if f != '.DS_Store':
                if os.path.islink(source_file):
                    os.symlink(os.readlink(source_file), target_file)
                else:
                    shutil.copy(source_file, target_file)

        # copyrights
        copyrights_dir = join(fastr_dir, 'mx.fastr', 'copyrights')
        with open(join(output_dir, 'COPYRIGHT'), 'w') as outfile:
            for copyright_file in os.listdir(copyrights_dir):
                if basename(copyright_file).endswith('copyright.star'):
                    with open(join(copyrights_dir, copyright_file)) as infile:
                        data = infile.read()
                        outfile.write(data)
        # license/README
        shutil.copy(join(fastr_dir, 'LICENSE'), output_dir)
        shutil.copy(join(fastr_dir, 'README.md'), output_dir)

        # jar files for the launchers
        bin_dir = join(output_dir, 'bin')
        jars_dir = join(bin_dir, 'fastr_jars')
        if not os.path.exists(jars_dir):
            os.mkdir(jars_dir)

        # Copy all the jar files needed by the launchers
        for e in mx.classpath_entries('FASTR'):
            source_path = e.classpath_repr()
            if mx.is_cache_path(source_path):
                target_file_name = e.name.lower().replace('_', '-') + '.jar'
            else:
                target_file_name = basename(source_path)
            assert isfile(source_path)
            shutil.copy(source_path, join(jars_dir, target_file_name))

        # create the classpath string
        classpath = []
        for _, _, jars in os.walk(jars_dir):
            for jar in jars:
                classpath.append(join("$R_HOME/bin/fastr_jars", jar))
        classpath_string = ":".join(classpath)

        # replace the mx exec scripts with native Java launchers, setting the classpath from above
        bin_exec_dir = join(bin_dir, 'exec')
        r_launcher = join(self.subject.dir, 'src', 'R_launcher')
        template_dict = {'CLASSPATH': classpath_string}
        self._template(r_launcher, join(bin_exec_dir, 'R'), template_dict)
        shutil.rmtree(join(bin_dir, 'execRextras'))
        rscript_launcher = join(self.subject.dir, 'src', 'Rscript_launcher')
        self._template(rscript_launcher, join(bin_dir, 'Rscript'), template_dict)


class FastRArchiveParticipant:
    def __init__(self, dist):
        self.dist = dist

    def __opened__(self, arc, srcArc, services):
        pass

    def __add__(self, arcname, contents):  # pylint: disable=unexpected-special-method-signature
        return False

    def __addsrc__(self, arcname, contents):
        return False

    def __closing__(self):
        if "FASTR_RELEASE" in self.dist.name:
            assert isinstance(self.dist.deps[0], FastRReleaseProject)
            release_project = self.dist.deps[0]
            # the files copied in can be confused as source files by
            # e.g., mx copyright, so delete them, specifically the
            # include dir
            include_dir = join(release_project.dir, 'include')
            shutil.rmtree(include_dir)


def mx_post_parse_cmd_line(opts):
    for dist in _fastr_suite.dists:
        if isinstance(dist, mx.JARDistribution):
            dist.set_archiveparticipant(FastRArchiveParticipant(dist))


def mx_register_dynamic_suite_constituents(register_project, register_distribution):
    fastr_release_distribution = mx.JARDistribution(
        suite=_fastr_suite,
        name="FASTR_RELEASE",
        subDir=None,
        path="mxbuild/dists/<os>/<arch>/fastr-release.jar",
        sourcesPath=None,
        deps=["com.oracle.truffle.r.release"],
        mainClass=None,
        excludedLibs=[],
        distDependencies=[],
        javaCompliance=None,
        platformDependent=True,
        theLicense=None
    )
    fastr_release_distribution.description = "a binary release of FastR"

    fastr_graalvm_release = mx.NativeTARDistribution(
        suite=_fastr_suite,
        name="FASTR_GRAALVM_RELEASE",
        deps=["com.oracle.truffle.r.release"],
        path=None,
        excludedLibs=[],
        platformDependent=True,
        theLicense=None,
        relpath=True,
        output=None,
        overlaps=["FASTR_RELEASE"]
    )

    register_distribution(fastr_release_distribution)
    register_distribution(fastr_graalvm_release)


    fastr_graalvm_release_support = mx.LayoutTARDistribution(
        suite=_fastr_suite,
        name="FASTR_GRAALVM_SUPPORT",
        deps=[],
        layout={
            "./": [
                {
                    "source_type": "extracted-dependency",
                    "dependency": "FASTR_GRAALVM_RELEASE",
                    "path": "*",
                    "exclude": [
                        "COPYRIGHT",
                        "LICENSE",
                        "README.md",
                        "bin/Rscript",
                        "bin/fastr_jars",
                        "bin/exec/R",
                    ],
                },
                {
                    "source_type": "extracted-dependency",
                    "dependency": "FASTR_GRAALVM_RELEASE",
                    "path": "bin/fastr_jars/*",
                    "exclude": [
                        "bin/fastr_jars/fastr.jar",
                        "bin/fastr_jars/truffle*",
                        "bin/fastr_jars/graal-sdk*",
                    ],
                },
                "dependency:fastr:FASTR_LAUNCHER",
            ],
            "LICENSE_FASTR" : "file:LICENSE",
            "3rd_party_licenses_fastr.txt" : "file:3rd_party_licenses.txt",
            "README_FASTR": "extracted-dependency:fastr:FASTR_GRAALVM_RELEASE/README.md",
            "bin/Rscript": "file:com.oracle.truffle.r.release/src/Rscript_launcher",
            "bin/exec/R": "file:com.oracle.truffle.r.release/src/R_launcher",
            "native-image.properties": "file:mx.fastr/native-image.properties",
        },
        path=None,
        platformDependent=True,
        theLicense=None
    )
    fastr_graalvm_release_support.description = "FastR support distribution for the GraalVM"
    register_distribution(fastr_graalvm_release_support)

mx_sdk.register_graalvm_component(mx_sdk.GraalVmLanguage(
    suite=_fastr_suite,
    name='FastR',
    short_name='R',
    license_files=['LICENSE_FASTR'],
    third_party_license_files=['3rd_party_licenses_fastr.txt'],
    dependencies=['Truffle', 'Sulong', 'LLVM.org toolchain'],
    truffle_jars=['fastr:FASTR'],
    support_distributions=['fastr:FASTR_GRAALVM_SUPPORT'],
    provided_executables=[
        'bin/Rscript',
        'bin/R',
    ],
    include_in_polyglot=False,
    launcher_configs=[
        mx_sdk.LanguageLauncherConfig(
            destination='bin/RMain',
            jar_distributions=['fastr:FASTR_LAUNCHER'],
            main_class='com.oracle.truffle.r.launcher.RMain',
            build_args=[],
            language='R',
            is_main_launcher=False,
            default_symlinks=False,
        )
    ],
    post_install_msg="NOTES:\n---------------\n" +
            "FastR should work out of the box on most Linux distributions and recent MacOS versions. " +
            "Run the following script to check FastR requirements and create a personal R packages library directory: \n" +
            "    ${graalvm_languages_dir}/R/bin/configure_fastr\n\n" +
            "The R component comes without native image by default. If you wish to build the native image, " +
            "which provides faster startup, but slightly slower peak performance, then run the following:\n" +
            "    gu rebuild-images R\n\n" +
            "The native image is then used by default. Pass '--jvm' flag to the R or Rscript launcher to " +
            "use JVM instead of the native image. Note that the native image is not stable yet and is intended for evaluation " +
            "and experiments for curious users. Some features may not work in the native image mode. " + 
            "Most notably, the --polyglot switch works only in JVM mode (when --jvm is used).\n\n" +
            "See http://www.graalvm.org/docs/reference-manual/languages/r for more."
))
