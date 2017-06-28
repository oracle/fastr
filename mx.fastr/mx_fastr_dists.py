#
# Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
import mx
import mx_fastr
import os, string, shutil
from os.path import join

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

class FastRReleaseProject(FastRProjectAdapter):
    '''
    Custom class for creating the FastR release project, which supports the
    FASTR_RELEASE distribution.
    '''
    def __init__(self, suite, name, deps, workingSets, theLicense, **args):
        FastRProjectAdapter.__init__(self, suite, name, deps, workingSets, theLicense)

    def getResults(self):
        results = []
        if os.environ.has_key('FASTR_RELEASE'):
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
        if not os.environ.has_key('FASTR_RELEASE'):
            mx.log('FastR: set FASTR_RELEASE to update release project')
            return
        # copy the release directories
        output_dir = self.subject.dir
        fastr_dir = mx_fastr._fastr_suite.dir
        for d in ['bin', 'include', 'lib', 'library', 'etc', 'share', 'doc']:
            target_dir = join(output_dir, d)
            if os.path.exists(target_dir):
                shutil.rmtree(target_dir)
            shutil.copytree(join(fastr_dir, d), target_dir)

        # copyrights
        copyrights_dir = join(fastr_dir, 'mx.fastr', 'copyrights')
        with open(join(output_dir, 'COPYRIGHT'), 'w') as outfile:
            for copyright_file in os.listdir(copyrights_dir):
                basename = os.path.basename(copyright_file)
                if basename.endswith('copyright.star'):
                    with open(join(copyrights_dir, copyright_file)) as infile:
                        data = infile.read()
                        outfile.write(data)
        # license/README
        shutil.copy(join(fastr_dir, 'LICENSE'), output_dir)
        shutil.copy(join(fastr_dir, 'README.md'), output_dir)

        # canonicalize R_HOME_DIR in bin/R
        bin_dir = join(output_dir, 'bin')
        rcmd = join(bin_dir, 'R')
        # R is the generic shell script (taken essentially verbatim from GNU R)
        with open(rcmd) as f:
            lines = f.readlines()
        with open(rcmd, 'w') as f:
            for line in lines:
                if line.startswith('R_HOME_DIR='):
                    f.write('R_HOME_DIR="$(dirname $0)/.."\n')
                    # produces a canonical path
                    line = 'R_HOME_DIR="$(unset CDPATH && cd ${R_HOME_DIR} && pwd)"\n'
                f.write(line)
        # jar files for the launchers
        jars_dir = join(bin_dir, 'fastr_jars')
        if not os.path.exists(jars_dir):
            os.mkdir(jars_dir)
        fastr_classfiles = dict()

        # visitor to collect/copy all the classes/jar files needed by the launchers
        def dep_visit(dep, edge):
            if isinstance(dep, mx.JARDistribution):
                shutil.copy(join(dep.suite.dir, dep.path), jars_dir)
            elif isinstance(dep, mx.Library):
                if not dep.name.lower() == 'jdk_tools':
                    jar_name = dep.name.lower() + '.jar'
                    shutil.copyfile(join(dep.suite.dir, dep.path), join(jars_dir, jar_name))
            elif isinstance(dep, mx.JavaProject):
                if 'com.oracle.truffle.r' in dep.name:
                    classfiles_dir = dep.output_dir()
                    for root, _, classfiles in os.walk(classfiles_dir):
                        for classfile in classfiles:
                            fastr_classfiles[os.path.relpath(join(root, classfile), classfiles_dir)] = join(root, classfile)

        self.subject.walk_deps(visit=dep_visit)

        # create the fastr.jar file
        with mx.Archiver(join(jars_dir, 'fastr.jar')) as arc:
            arc.zf.writestr("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\n")
            for arcname, path in fastr_classfiles.iteritems():
                with open(path, 'r') as f:
                    contents = f.read()
                arc.zf.writestr(arcname, contents)

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

class FastRNativeRecommendedProject(mx.NativeProject):
    '''
    This finesses an ordering problem on installing the recommended R packages.
    These must be installed by FastR using bin/R CMD INSTALL. That will invoke a
    nested 'mx R' invocation which requires the FASTR distribution to be available.
    However, this dependency cannt be specified in the suite.py file so we achieve
    it here by ensuring that it is built prior to the native.recommended project.
    '''
    def __init__(self, suite, name, deps, workingSets, theLicense, **args):
        mx.NativeProject.__init__(self, suite, name, None, [], deps, workingSets, None, None, join(suite.dir, name), theLicense)

    def getBuildTask(self, args):
        return NativeRecommendedBuildTask(self, args)

class NativeRecommendedBuildTask(mx.NativeBuildTask):
    def __init__(self, project, args):
        mx.NativeBuildTask.__init__(self, args, project)

    def build(self):
        # must archive FASTR before build so that nested mx R CMD INSTALL can execute
        mx.archive(['@FASTR'])
        mx.NativeBuildTask.build(self)


class FastRArchiveParticipant:
    def __init__(self, dist):
        self.dist = dist

    def __opened__(self, arc, srcArc, services):
            # The release project states dependencies on the java projects in order
            # to ensure they are built first. Therefore, the JarDistribution code
            # will include all their class files at the top-level of the jar by default.
            # Since we have already encapsulated the class files in 'fastr_jars/fastr.jar' we
            # suppress their inclusion here by resetting the deps field. A bit of a hack.
        if "FASTR_RELEASE" in self.dist.name:
            assert isinstance(self.dist.deps[0], FastRReleaseProject)
            self.release_project = self.dist.deps[0]
            self.dist.deps[0].deps = []

    def __add__(self, arcname, contents):
        return False

    def __addsrc__(self, arcname, contents):
        return False

    def __closing__(self):
        if "FASTR_RELEASE" in self.dist.name and os.environ.has_key('FASTR_RELEASE'):
            # the files copied  in can be confused as source files by
            # e.g., mx copyright, so delete them, specifically thne
            # include dir
            include_dir = join(self.release_project.dir, 'include')
            shutil.rmtree(include_dir)

def mx_post_parse_cmd_line(opts):
    if os.environ.has_key('FASTR_RFFI'):
        val = os.environ['FASTR_RFFI']
    else:
        val = ""
    mx.instantiateDistribution('FASTR_RELEASE<rffi>', dict(rffi=val))

    for dist in mx_fastr._fastr_suite.dists:
        if isinstance(dist, mx.JARDistribution):
            dist.set_archiveparticipant(FastRArchiveParticipant(dist))

