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

'''
The pkgtest command operates in two modes:
1. In development mode it uses the FastR 'Rscript' command and the internal GNU R for test comparison
2. In production mode it uses the GraalVM 'Rscript' command and a GNU R loaded as a sibling suite. This is indicated
by the environment variable 'GRAALVM_FASTR' being set.

Evidently in case 2, there is the potential for a version mismatch between FastR and GNU R, and this is checked.

In either case all the output is placed in the fastr suite dir. Separate directories are used for FastR and GNU R package installs
and tests, namely 'lib.install.cran.{fastr,gnur}' and 'test.{fastr,gnur}' (sh syntax).
'''
from os.path import join, relpath
import shutil, os, re
import subprocess
import mx
import mx_fastr

quiet = False
graalvm = None

def _fastr_suite_dir():
    return mx_fastr._fastr_suite.dir

def _mx_gnur():
    return mx.suite('gnur')

def _gnur_rscript():
    return _mx_gnur().extensions._gnur_rscript_path()

def _graalvm_rscript():
    assert graalvm is not None
    return join(graalvm, 'bin', 'Rscript')

def _graalvm():
    global graalvm
    if graalvm is None:
        if os.environ.has_key('GRAALVM_FASTR'):
            graalvm = os.environ['GRAALVM_FASTR']
            # version check
            gnur_version = _mx_gnur().extensions.r_version().split('-')[1]
            graalvm_version = subprocess.check_output([_graalvm_rscript(), '--version'], stderr=subprocess.STDOUT).rstrip()
            if not gnur_version in graalvm_version:
                mx.abort('graalvm R version does not match gnur suite')
    return graalvm

def _create_libinstall(rvm, test_installed):
    '''
    Create lib.install.cran.<rvm>/install.tmp.<rvm>/test.<rvm> for <rvm>: fastr or gnur
    If use_installed_pkgs is True, assume lib.install exists and is populated (development)
    '''
    libinstall = join(_fastr_suite_dir(), "lib.install.cran." + rvm)
    if not test_installed:
        # make sure its empty
        shutil.rmtree(libinstall, ignore_errors=True)
        os.mkdir(libinstall)
    install_tmp = join(_fastr_suite_dir(), "install.tmp." + rvm)
#    install_tmp = join(_fastr_suite_dir(), "install.tmp")
    shutil.rmtree(install_tmp, ignore_errors=True)
    os.mkdir(install_tmp)
    testdir = join(_fastr_suite_dir(), "test." + rvm)
    shutil.rmtree(testdir, ignore_errors=True)
    os.mkdir(testdir)
    return libinstall, install_tmp, testdir

def _log_step(state, step, rvariant):
    if not quiet:
        print "{0} {1} with {2}".format(state, step, rvariant)

def _cran_test_project():
    return 'com.oracle.truffle.r.test.cran'

def _cran_test_project_dir():
    return mx.project(_cran_test_project()).dir

def installpkgs(args):
    _installpkgs(args)

def _installpkgs_script():
    cran_test = _cran_test_project_dir()
    return join(cran_test, 'r', 'install.cran.packages.R')

def _installpkgs(args, **kwargs):
    '''
    Runs the R script that does package/installation and testing.
    If we are running in a binary graalvm environment, which is indicated
    by the GRAALVM_FASTR environment variable, we can't use mx to invoke
    FastR, but instead have to invoke the command directly.
    '''
    script = _installpkgs_script()
    if _graalvm() is None:
        return mx_fastr.rscript([script] + args, **kwargs)
    else:
        return mx.run([_graalvm_rscript(), script] + args, **kwargs)


def pkgtest(args):
    '''
    Package installation/testing.
    rc: 0 for success; 1: install fail, 2: test fail, 3: install&test fail
    '''

    test_installed = '--no-install' in args
    fastr_libinstall, fastr_install_tmp, fastr_testdir = _create_libinstall('fastr', test_installed)
    gnur_libinstall, gnur_install_tmp, gnur_testdir = _create_libinstall('gnur', test_installed)

    if "--quiet" in args:
        global quiet
        quiet = True

    install_args = args

    class OutputCapture:
        def __init__(self):
            self.install_data = None
            self.pkg = None
            self.mode = None
            self.start_install_pattern = re.compile(r"^BEGIN processing: (?P<package>[a-zA-Z0-9\.\-]+) .*")
            self.test_pattern = re.compile(r"^(?P<status>BEGIN|END) testing: (?P<package>[a-zA-Z0-9\.\-]+) .*")
            self.time_pattern = re.compile(r"^TEST_TIME: (?P<package>[a-zA-Z0-9\.\-]+) (?P<time>[0-9\.\-]+) .*")
            self.status_pattern = re.compile(r"^(?P<package>[a-zA-Z0-9\.\-]+): (?P<status>OK|FAILED).*")
            self.install_data = dict()
            self.install_status = dict()
            self.test_info = dict()

        def __call__(self, data):
            print data,
            if data == "BEGIN package installation\n":
                self.mode = "install"
                return
            elif data == "BEGIN install status\n":
                self.mode = "install_status"
                return
            elif data == "BEGIN package tests\n":
                self.mode = "test"
                return

            if self.mode == "install":
                start_install = re.match(self.start_install_pattern, data)
                if start_install:
                    pkg_name = start_install.group(1)
                    self.pkg = pkg_name
                    self.install_data[self.pkg] = ""
                if self.pkg:
                    self.install_data[self.pkg] += data
            elif self.mode == "install_status":
                if data == "END install status\n":
                    self.mode = None
                    return
                status = re.match(self.status_pattern, data)
                pkg_name = status.group(1)
                self.install_status[pkg_name] = status.group(2) == "OK"
            elif self.mode == "test":
                test_match = re.match(self.test_pattern, data)
                if test_match:
                    begin_end = test_match.group(1)
                    pkg_name = test_match.group(2)
                    if begin_end == "END":
                        _get_test_outputs('fastr', pkg_name, self.test_info)
                else:
                    time_match = re.match(self.time_pattern, data)
                    if time_match:
                        pkg_name = time_match.group(1)
                        test_time = time_match.group(2)
                        with open(join(_pkg_testdir('fastr', pkg_name), 'test_time'), 'w') as f:
                            f.write(test_time)
    env = os.environ.copy()
    env["TMPDIR"] = fastr_install_tmp
    env['R_LIBS'] = fastr_libinstall
    if not env.has_key('FASTR_PROCESS_TIMEOUT'):
        env['FASTR_PROCESS_TIMEOUT'] = '5'
    env['FASTR_OPTION_PrintErrorStacktracesToFile'] = 'false'
    env['FASTR_OPTION_PrintErrorStacktraces'] = 'true'

    # TODO enable but via installing Suggests
    #_install_vignette_support('FastR', env)

    out = OutputCapture()
    # install and test the packages, unless just listing versions
    if not '--list-versions' in install_args:
        install_args += ['--run-tests']
        install_args += ['--testdir', 'test.fastr']
        if not '--print-install-status' in install_args:
            install_args += ['--print-install-status']

    _log_step('BEGIN', 'install/test', 'FastR')
    # Currently installpkgs does not set a return code (in install.cran.packages.R)
    rc = _installpkgs(install_args, nonZeroIsFatal=False, env=env, out=out, err=out)
    if rc == 100:
        # fatal error connecting to package repo
        mx.abort(rc)

    rc = 0
    for status in out.install_status.itervalues():
        if not status:
            rc = 1
    _log_step('END', 'install/test', 'FastR')

    single_pkg = len(out.install_status) == 1
    install_failure = single_pkg and rc == 1
    if '--run-tests' in install_args and not install_failure:
        # in order to compare the test output with GnuR we have to install/test the same
        # set of packages with GnuR
        ok_pkgs = [k for k, v in out.install_status.iteritems() if v]
        _gnur_install_test(ok_pkgs, gnur_libinstall, gnur_install_tmp)
        _set_test_status(out.test_info)
        print 'Test Status'
        for pkg, test_status in out.test_info.iteritems():
            if test_status.status != "OK":
                rc = rc | 2
            print '{0}: {1}'.format(pkg, test_status.status)

        # tar up the test results
        fastr_tar = join(_fastr_suite_dir, fastr_testdir + '.tar')
        subprocess.call(['tar', 'cf', fastr_tar, os.path.basename(fastr_testdir)])
        subprocess.call(['gzip', fastr_tar])
        gnur_tar = join(_fastr_suite_dir, gnur_testdir + '.tar')
        subprocess.call(['tar', 'cf', gnur_tar, os.path.basename(gnur_testdir)])
        subprocess.call(['gzip', gnur_tar])

    shutil.rmtree(fastr_install_tmp, ignore_errors=True)
    return rc

class TestFileStatus:
    '''
    Records the status of a test file. status is either "OK" or "FAILED".
    The latter means that the file had a .fail extension.
    '''
    def __init__(self, status, abspath):
        self.status = status
        self.abspath = abspath

class TestStatus:
    '''Records the test status of a package. status ends up as either "OK" or "FAILED",
    unless GnuR also failed in which case it stays as UNKNOWN.
    The testfile_outputs dict is keyed by the relative path of the output file to
    the 'test/pkgname' directory. The value is an instance of TestFileStatus.
    '''
    def __init__(self):
        self.status = "UNKNOWN"
        self.testfile_outputs = dict()

def _pkg_testdir(rvm, pkg_name):
    return join(_fastr_suite_dir(), 'test.' + rvm, pkg_name)

def _get_test_outputs(rvm, pkg_name, test_info):
    pkg_testdir = _pkg_testdir(rvm, pkg_name)
    for root, _, files in os.walk(pkg_testdir):
        if not test_info.has_key(pkg_name):
            test_info[pkg_name] = TestStatus()
        for f in files:
            ext = os.path.splitext(f)[1]
            if f == 'test_time' or ext == '.R' or ext == '.prev':
                continue
            # suppress .pdf's for now (we can't compare them)
            if ext == '.pdf':
                continue
            if ext == '.save':
                continue
            status = "OK"
            if ext == '.fail':
                # some fatal error during the test
                status = "FAILED"
                f = os.path.splitext(f)[0]

            absfile = join(root, f)
            relfile = relpath(absfile, pkg_testdir)
            test_info[pkg_name].testfile_outputs[relfile] = TestFileStatus(status, absfile)

def _install_vignette_support(rvm, env):
    # knitr is needed for vignettes, but FastR  can't handle it yet
    if rvm == 'FastR':
        return
    _log_step('BEGIN', 'install vignette support', rvm)
    args = [_installpkgs_script(), '--ignore-blacklist', '^rmarkdown$|^knitr$']
    mx_fastr.gnu_rscript(args, env)
    _log_step('END', 'install vignette support', rvm)

def _gnur_install_test(pkgs, gnur_libinstall, gnur_install_tmp):
    gnur_packages = join(_fastr_suite_dir(), 'gnur.packages')
    with open(gnur_packages, 'w') as f:
        for pkg in pkgs:
            f.write(pkg)
            f.write('\n')
    env = os.environ.copy()
    env["TMPDIR"] = gnur_install_tmp
    env['R_LIBS'] = gnur_libinstall

    # TODO enable but via installing Suggests
    # _install_vignette_support('GnuR', env)

    args = []
    if _graalvm():
        args += [_gnur_rscript()]
    args += [_installpkgs_script()]
    args += ['--pkg-filelist', gnur_packages]
    args += ['--run-tests']
    args += ['--run-mode', 'internal']
    args += ['--ignore-blacklist']
    args += ['--testdir', 'test.gnur']
    _log_step('BEGIN', 'install/test', 'GnuR')
    if _graalvm():
        mx.run(args, nonZeroIsFatal=False, env=env)
    else:
        mx_fastr.gnu_rscript(args, env=env)
    _log_step('END', 'install/test', 'GnuR')

def _set_test_status(fastr_test_info):
    def _failed_outputs(outputs):
        '''
        return True iff outputs has any .fail files
        '''
        for _, testfile_status in outputs.iteritems():
            if testfile_status.status == "FAILED":
                return True
        return False

    gnur_test_info = dict()
    for pkg, _ in fastr_test_info.iteritems():
        _get_test_outputs('gnur', pkg, gnur_test_info)

    # gnur is definitive so drive off that
    for pkg in gnur_test_info.keys():
        print 'BEGIN checking ' + pkg
        gnur_test_status = gnur_test_info[pkg]
        fastr_test_status = fastr_test_info[pkg]
        gnur_outputs = gnur_test_status.testfile_outputs
        fastr_outputs = fastr_test_status.testfile_outputs
        if _failed_outputs(gnur_outputs):
            # What this likely means is that some native package is not
            # installed on the system so GNUR can't run the tests.
            # Ideally this never happens.
            print "{0}: GnuR test had .fail outputs".format(pkg)

        if _failed_outputs(fastr_outputs):
            # In addition to the similar comment for GNU R, this can happen
            # if, say, the JVM crashes (possible with native code packages)
            print "{0}: FastR test had .fail outputs".format(pkg)
            fastr_test_status.status = "FAILED"

        # Now for each successful GNU R output we compare content (assuming FastR didn't fail)
        for gnur_test_output_relpath, gnur_testfile_status in gnur_outputs.iteritems():
            # Can't compare if either GNUR or FastR failed
            if gnur_testfile_status.status == "FAILED":
                break

            if not gnur_test_output_relpath in fastr_outputs:
                # FastR crashed on this test
                fastr_test_status.status = "FAILED"
                print "{0}: FastR is missing output file: {1}".format(pkg, gnur_test_output_relpath)
                break

            fastr_testfile_status = fastr_outputs[gnur_test_output_relpath]
            if fastr_testfile_status.status == "FAILED":
                break

            gnur_content = None
            with open(gnur_testfile_status.abspath) as f:
                gnur_content = f.readlines()
            fastr_content = None
            with open(fastr_testfile_status.abspath) as f:
                fastr_content = f.readlines()

            result = _fuzzy_compare(gnur_content, fastr_content)
            if result == -1:
                print "{0}: content malformed: {1}".format(pkg, gnur_test_output_relpath)
                fastr_test_status.status = "INDETERMINATE"
                break
            if result != 0:
                fastr_test_status.status = "FAILED"
                fastr_testfile_status.status = "FAILED"
                print "{0}: FastR output mismatch: {1}".format(pkg, gnur_test_output_relpath)
                break
        # we started out as UNKNOWN
        if not (fastr_test_status.status == "INDETERMINATE" or fastr_test_status.status == "FAILED"):
            fastr_test_status.status = "OK"

        # write out a file with the test status for each output (that exists)
        with open(join(_pkg_testdir('fastr', pkg), 'testfile_status'), 'w') as f:
            for fastr_relpath, fastr_testfile_status in fastr_outputs.iteritems():
                if fastr_testfile_status.status == "FAILED":
                    relpath = fastr_relpath + ".fail"
                else:
                    relpath = fastr_relpath

                if os.path.exists(join(_pkg_testdir('fastr', pkg), relpath)):
                    f.write(relpath)
                    f.write(' ')
                    f.write(fastr_testfile_status.status)
                    f.write('\n')

        print 'END checking ' + pkg

def _find_start(content):
    marker = "Type 'q()' to quit R."
    for i in range(len(content)):
        line = content[i]
        if marker in line:
            return i + 1
    return None

def _find_end(content):
    marker = "Time elapsed:"
    for i in range(len(content)):
        line = content[i]
        if marker in line:
            return i - 1
    # not all files have a Time elapsed:
    return len(content) - 1

def _fuzzy_compare(gnur_content, fastr_content):
    gnur_start = _find_start(gnur_content)
    gnur_end = _find_end(gnur_content)
    fastr_start = _find_start(fastr_content)
    fastr_len = len(fastr_content)
    if not gnur_start or not gnur_end or not fastr_start:
        return -1
    gnur_start = gnur_start + 1 # Gnu has extra empty line
    result = 0
    i = gnur_start
    while i + gnur_start < gnur_end:
        gnur_line = gnur_content[i + gnur_start]
        if i + fastr_start >= fastr_len:
            result = 1
            break

        fastr_line = fastr_content[i + fastr_start]
        if gnur_line != fastr_line:
            result = 1
            break
        i = i + 1
    return result


