#
# Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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


class FastRNativeProject(FastRProjectAdapter):
    '''
    Custom class for building the com.oracle.truffle.r.native project.
    The customization is to support the creation of an exact FASTR_NATIVE_DEV distribution.
    '''
    def __init__(self, suite, name, deps, workingSets, theLicense, **args):
        FastRProjectAdapter.__init__(self, suite, name, deps, workingSets, theLicense)

    def getBuildTask(self, args):
        return mx.NativeBuildTask(args, self)

    def _get_gnur_files(self, gnur_dir, files, results):
        for f in files:
            results.append(join(self.dir, gnur_dir, f))

    def getResults(self):
        '''
        Capture all the files from the com.oracle.truffle.r.native project that are needed
        in an alternative implementation of the R FFI. This includes some files from GNU R.
        This code has to be kept in sync with the FFI implementation.
        '''
        # plain files
        results = [join(self.dir, "platform.mk")]
        gnur = join('gnur', mx_fastr.r_version())
        gnur_appl = join(gnur, 'src', 'appl')
        self._get_gnur_files(gnur_appl, ['pretty.c', 'interv.c'], results)
        gnur_main = join(gnur, 'src', 'main')
        self._get_gnur_files(gnur_main, ['colors.c', 'devices.c', 'engine.c', 'format.c', 'graphics.c',
                                         'plot.c', 'plot3d.c', 'plotmath.c', 'rlocale.c', 'sort.c'], results)
        # these files are not compiled, just "included"
        self._get_gnur_files(gnur_main, ['xspline.c', 'rlocale_data.h'], results)
        # directories
        for d in ["fficall/src/common", "fficall/src/include", "fficall/src/variable_defs"]:
            self._get_files(d, results)

        def is_dot_h(f):
            ext = os.path.splitext(f)[1]
            return ext == '.h'

        # just the .h files from 'include'
        self._get_files('include', results, is_dot_h)

        # tools for alternate impl of gramRd.c
        gnur_tools = join(gnur, 'library', 'tools')
        self._get_files(gnur_tools, results)
        gnur_tools_src = join(gnur, 'src', 'library', 'tools', 'src')
        for f in ['gramRd.c', 'init.c', 'tools.h']:
            results.append(join(self.dir, gnur_tools_src, f))
        for f in ['lib.mk', 'Makefile', 'tools/src/tools_dummy.c', 'tools/src/gramRd_fastr.h', 'tools/Makefile']:
            results.append(join(self.dir, 'library', f))

        # selected headers from GNU R source
        with open(join(self.dir, 'fficall/src/include/gnurheaders.mk')) as f:
            lines = f.readlines()
            for line in lines:
                if '$(GNUR_HOME)' in line:
                    parts = line.split(' ')
                    results.append(join(self.dir, parts[2].rstrip().replace('$(GNUR_HOME)', gnur)))

        def is_ddot_o(f):
            ext = os.path.splitext(f)[1]
            return f[0] == 'd' and ext == '.o'

        # binary files from GNU R
        self._get_files(gnur_appl, results, is_ddot_o)

        return results

class FastRTestNativeProject(FastRProjectAdapter):
    '''
    Custom class for building the com.oracle.truffle.r.native project.
    The customization is to support the creation of an exact FASTR_NATIVE_DEV distribution.
    '''
    def __init__(self, suite, name, deps, workingSets, theLicense, **args):
        FastRProjectAdapter.__init__(self, suite, name, deps, workingSets, theLicense)

    def getBuildTask(self, args):
        return mx.NativeBuildTask(args, self)

    def getResults(self):
        '''
        Capture all the files from the com.oracle.truffle.r.test.native project that are needed
        for running unit tests in an alternate implementation.
        '''
        # plain files
        results = []

        self._get_files(join('packages', 'recommended'), results)

        fastr_packages = []
        fastr_packages_dir = join(self.dir, 'packages')
        for root, dirs, _ in os.walk(fastr_packages_dir):
            for d in dirs:
                if d == 'recommended':
                    continue
                if os.path.isdir(join(root, d)):
                    fastr_packages.append(d)
            break
        for p in fastr_packages:
            results.append(join(fastr_packages_dir, p, 'lib', p + '.tar'))

        results.append(join(self.dir, 'urand', 'lib', 'liburand.so'))
        return results

class FastRReleaseProject(FastRProjectAdapter):
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
        if os.environ.has_key('FASTR_NO_RELEASE'):
            mx.log('FastR: not updating release project')
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

class FastRArchiveParticipant:
    def __init__(self, dist):
        self.dist = dist

    def __opened__(self, arc, srcArc, services):
            # The release project states dependencies on the java projects in order
            # to ensure they are built first. Therefore, the JarDistribution code
            # will include all their class files at the top-level of the jar by default.
            # Since we have already encapsulated the class files in 'fastr_jars/fastr.jar' we
            # suppress their inclusion here by resetting the deps field. A bit of a hack.
        if self.dist.name == "FASTR_RELEASE":
            assert isinstance(self.dist.deps[0], FastRReleaseProject)
            self.release_project = self.dist.deps[0]
            self.dist.deps[0].deps = []

    def __add__(self, arcname, contents):
        return False

    def __addsrc__(self, arcname, contents):
        return False

    def __closing__(self):
        if self.dist.name == "FASTR_RELEASE":
            # the files copied  in can be confused as source files by
            # e.g., mx copyright, so delete them, specifically thne
            # include dir
            include_dir = join(self.release_project.dir, 'include')
            shutil.rmtree(include_dir)


def mx_post_parse_cmd_line(opts):
    for dist in mx_fastr._fastr_suite.dists:
        dist.set_archiveparticipant(FastRArchiveParticipant(dist))
