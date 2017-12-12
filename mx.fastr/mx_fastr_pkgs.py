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

'''
The pkgtest command operates in two modes:
1. In development mode it uses the FastR 'Rscript' command and the internal GNU R for test comparison
2. In production mode it uses the GraalVM 'Rscript' command and a GNU R loaded as a sibling suite. This is indicated
by the environment variable 'FASTR_GRAALVM' being set. (GRAALVM_FASTR is also accepted for backwards cmpatibility)

Evidently in case 2, there is the potential for a version mismatch between FastR and GNU R, and this is checked.

In either case all the output is placed in the fastr suite dir. Separate directories are used for FastR and GNU R package installs
and tests, namely 'lib.install.packages.{fastr,gnur}' and 'test.{fastr,gnur}' (sh syntax).
'''
from os.path import join, relpath
from datetime import datetime
import shutil, os, re
import subprocess
import hashlib
import mx
import mx_fastr

quiet = False
graalvm = None

def _fastr_suite_dir():
    return mx_fastr._fastr_suite.dir

def _mx_gnur():
    return mx.suite('gnur')

def _gnur_rscript():
    '''
    returns path to Rscript in sibling gnur directory
    '''
    return _mx_gnur().extensions._gnur_rscript_path()

def _gnur_include_path():
    gnur_include_p = None
    if _graalvm():
        gnur_include_p = join(_mx_gnur().dir, 'gnur', _mx_gnur().extensions.r_version(), 'include')
    else:
        gnur_include_p = join(mx_fastr._gnur_path(), "include")
    return gnur_include_p

def _fastr_include_path():
    return join(_fastr_suite_dir(), 'include')

def _graalvm_rscript():
    assert graalvm is not None
    return join(graalvm, 'bin', 'Rscript')

def _check_graalvm():
    if os.environ.has_key('FASTR_GRAALVM'):
        return os.environ['FASTR_GRAALVM']
    elif os.environ.has_key('GRAALVM_FASTR'):
        return os.environ['GRAALVM_FASTR']
    else:
        return None

def _graalvm():
    global graalvm
    if graalvm is None:
        graalvm = _check_graalvm()
        if graalvm:
            # version check
            gnur_version = _mx_gnur().extensions.r_version().split('-')[1]
            graalvm_version = subprocess.check_output([_graalvm_rscript(), '--version'], stderr=subprocess.STDOUT).rstrip()
            if not gnur_version in graalvm_version:
                mx.abort('graalvm R version does not match gnur suite')
    return graalvm

def _create_libinstall(rvm, test_installed):
    '''
    Create lib.install.packages.<rvm>/install.tmp.<rvm>/test.<rvm> for <rvm>: fastr or gnur
    If use_installed_pkgs is True, assume lib.install exists and is populated (development)
    '''
    libinstall = join(_fastr_suite_dir(), "lib.install.packages." + rvm)
    if not test_installed:
        # make sure its empty
        shutil.rmtree(libinstall, ignore_errors=True)
        os.mkdir(libinstall)
    install_tmp = join(_fastr_suite_dir(), "install.tmp." + rvm)
#    install_tmp = join(_fastr_suite_dir(), "install.tmp")
    shutil.rmtree(install_tmp, ignore_errors=True)
    os.mkdir(install_tmp)
    _create_testdot(rvm)
    return libinstall, install_tmp

def _create_testdot(rvm):
    testdir = join(_fastr_suite_dir(), "test." + rvm)
    shutil.rmtree(testdir, ignore_errors=True)
    os.mkdir(testdir)
    return testdir

def _log_timestamp():
    if not quiet:
        print "timestamp: {0}".format(str(datetime.now()))

def _log_step(state, step, rvariant):
    if not quiet:
        print "{0} {1} with {2}".format(state, step, rvariant)
        _log_timestamp()

def _packages_test_project():
    return 'com.oracle.truffle.r.test.packages'

def _packages_test_project_dir():
    return mx.project(_packages_test_project()).dir

def _ensure_R_on_PATH(env, bindir):
    '''
    Some packages (e.g. stringi) require that 'R' is actually on the PATH
    '''
    env['PATH'] = join(bindir) + os.pathsep + os.environ['PATH']


def installpkgs(args):
    _installpkgs(args)

def _installpkgs_script():
    packages_test = _packages_test_project_dir()
    return join(packages_test, 'r', 'install.packages.R')

def _installpkgs(args, **kwargs):
    '''
    Runs the R script that does package/installation and testing.
    If we are running in a binary graalvm environment, which is indicated
    by the FASTR_GRAALVM environment variable, we can't use mx to invoke
    FastR, but instead have to invoke the command directly.
    '''
    if kwargs.has_key('env'):
        env = kwargs['env']
    else:
        env = os.environ.copy()
        kwargs['env'] = env

    script = _installpkgs_script()
    if _graalvm() is None:
        _ensure_R_on_PATH(env, join(_fastr_suite_dir(), 'bin'))
        return mx_fastr.rscript([script] + args, **kwargs)
    else:
        _ensure_R_on_PATH(env, os.path.dirname(_graalvm_rscript()))
        return mx.run([_graalvm_rscript(), script] + args, **kwargs)

_pta_main_class = 'com.oracle.truffle.r.test.packages.analyzer.PTAMain'

def _pta_project():
    return 'com.oracle.truffle.r.test.packages.analyzer'

def pta(args, **kwargs):
    '''
    Run analysis for package installation/testing results.
    '''
    vmArgs = mx.get_runtime_jvm_args(_pta_project())
    vmArgs += [_pta_main_class]
    mx.run_java(vmArgs + args)

def pkgtest(args):
    '''
    Package installation/testing.
    rc: 0 for success; 1: install fail, 2: test fail, 3: install&test fail
    '''

    test_installed = '--no-install' in args
    fastr_libinstall, fastr_install_tmp = _create_libinstall('fastr', test_installed)
    gnur_libinstall, gnur_install_tmp = _create_libinstall('gnur', test_installed)

    if "--quiet" in args:
        global quiet
        quiet = True

    install_args = list(args)

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
    env['R_LIBS_USER'] = fastr_libinstall
    env['FASTR_OPTION_PrintErrorStacktracesToFile'] = 'false'
    env['FASTR_OPTION_PrintErrorStacktraces'] = 'true'

    out = OutputCapture()
    # install and test the packages, unless just listing versions
    if not '--list-versions' in install_args:
        install_args += ['--run-tests']
        install_args += ['--testdir', 'test.fastr']
        if not '--print-install-status' in install_args:
            install_args += ['--print-install-status']

    # If '--cache-pkgs' is set, then also set the native API version value
    _set_pkg_cache_api_version(install_args, _fastr_include_path())

    _log_step('BEGIN', 'install/test', 'FastR')
    # Currently installpkgs does not set a return code (in install.packages.R)
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
        gnur_args = _args_to_forward_to_gnur(args)

        # If '--cache-pkgs' is set, then also set the native API version value
        _set_pkg_cache_api_version(gnur_args, _gnur_include_path())

        _gnur_install_test(gnur_args, ok_pkgs, gnur_libinstall, gnur_install_tmp)
        _set_test_status(out.test_info)
        print 'Test Status'
        for pkg, test_status in out.test_info.iteritems():
            if test_status.status != "OK":
                rc = rc | 2
            print '{0}: {1}'.format(pkg, test_status.status)

        diffdir = _create_testdot('diffs')
        for pkg, _ in out.test_info.iteritems():
            diff_file = join(diffdir, pkg)
            subprocess.call(['diff', '-r', _pkg_testdir('fastr', pkg), _pkg_testdir('gnur', pkg)], stdout=open(diff_file, 'w'))

    shutil.rmtree(fastr_install_tmp, ignore_errors=True)
    return rc

def _set_pkg_cache_api_version(arg_list, include_dir):
    '''
    Looks for argument '--cache-pkgs' and appends the native API version to the value list of this argument.
    '''
    if "--cache-pkgs" in arg_list:
        pkg_cache_values_idx = arg_list.index("--cache-pkgs") + 1
        if pkg_cache_values_idx < len(arg_list):
            arg_list[pkg_cache_values_idx] = arg_list[pkg_cache_values_idx] + ",version={0}".format(computeApiChecksum(include_dir))

class TestFileStatus:
    '''
    Records the status of a test file. status is either "OK" or "FAILED".
    The latter means that the file had a .fail extension.
    '''
    def __init__(self, status, abspath):
        self.status = status
        self.abspath = abspath
        self.report = 0, 1, 0

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
            # suppress .pdf's for now (we can't compare them)
            # ignore = ['.R', '.Rin', '.prev', '.bug', '.pdf', '.save']
            # if f == 'test_time' or ext in ignore:
            #     continue
            included = ['.Rout', '.fail']
            if f == 'test_time' or not ext in included:
                continue
            status = "OK"
            if ext == '.fail':
                # some fatal error during the test
                status = "FAILED"
                f = os.path.splitext(f)[0]

            absfile = join(root, f)
            relfile = relpath(absfile, pkg_testdir)
            test_info[pkg_name].testfile_outputs[relfile] = TestFileStatus(status, absfile)

def _args_to_forward_to_gnur(args):
    forwarded_args = ['--repos', '--run-mode', '--cache-pkgs']
    result = []
    i = 0
    while i < len(args):
        arg = args[i]
        if arg in forwarded_args:
            result.append(arg)
            i = i + 1
            result.append(args[i])
        i = i + 1
    return result

def _gnur_install_test(forwarded_args, pkgs, gnur_libinstall, gnur_install_tmp):
    '''
    Install/test with GNU R  exactly those packages that installed correctly with FastR.
    N.B. That means that regardless of how the packages were specified to pkgtest
    we always use a --pkg-filelist' arg to GNU R
    '''
    gnur_packages = join(_fastr_suite_dir(), 'gnur.packages')
    with open(gnur_packages, 'w') as f:
        for pkg in pkgs:
            f.write(pkg)
            f.write('\n')
    env = os.environ.copy()
    env["TMPDIR"] = gnur_install_tmp
    env['R_LIBS_USER'] = gnur_libinstall
    env["TZDIR"] = "/usr/share/zoneinfo/"

    args = []
    if _graalvm():
        args += [_gnur_rscript()]
    # forward any explicit args to pkgtest
    args += [_installpkgs_script()]
    args += forwarded_args
    args += ['--pkg-filelist', gnur_packages]
    args += ['--run-tests']
    args += ['--ignore-blacklist']
    args += ['--testdir', 'test.gnur']
    _log_step('BEGIN', 'install/test', 'GnuR')
    if _graalvm():
        _ensure_R_on_PATH(env, os.path.dirname(_gnur_rscript()))
        mx.run(args, nonZeroIsFatal=False, env=env)
    else:
        _ensure_R_on_PATH(env, mx_fastr._gnur_path())
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
                fastr_test_status.status = "INDETERMINATE"
                break

            if not gnur_test_output_relpath in fastr_outputs:
                # FastR crashed on this test
                fastr_test_status.status = "FAILED"
                print "{0}: FastR is missing output file: {1}".format(pkg, gnur_test_output_relpath)
                break

            fastr_testfile_status = fastr_outputs[gnur_test_output_relpath]
            if fastr_testfile_status.status == "FAILED":
                break

            print "Comparing {0} to {1}\n".format(gnur_testfile_status.abspath, fastr_testfile_status.abspath)

            gnur_content = None
            with open(gnur_testfile_status.abspath) as f:
                gnur_content = f.readlines()
            fastr_content = None
            with open(fastr_testfile_status.abspath) as f:
                fastr_content = f.readlines()

            # first, parse file and see if a known test framework has been used
            ok, skipped, failed = handle_output_file(fastr_content)
            if ok is not None:
                fastr_testfile_status.report = ok, skipped, failed
            else:
                result, n_tests_passed, n_tests_failed = _fuzzy_compare(gnur_content, fastr_content, gnur_testfile_status.abspath, fastr_testfile_status.abspath)
                print "result={0}, passed={1}, failed={2}\n".format(result, n_tests_passed, n_tests_failed)
                if result == -1:
                    print "{0}: content malformed: {1}".format(pkg, gnur_test_output_relpath)
                    fastr_test_status.status = "INDETERMINATE"
                    # we don't know how many tests are in there, so consider the whole file to be one big skipped test
                    fastr_testfile_status.report = 0, 1, 0
                    #break
                elif result != 0:
                    fastr_test_status.status = "FAILED"
                    fastr_testfile_status.status = "FAILED"
                    fastr_testfile_status.report = n_tests_passed, 0, n_tests_failed
                    print "{0}: FastR output mismatch: {1}".format(pkg, gnur_test_output_relpath)
                    #break
                else:
                    fastr_testfile_status.status = "OK"
                    fastr_testfile_status.report = n_tests_passed, 0, n_tests_failed


        # we started out as UNKNOWN
        if not (fastr_test_status.status == "INDETERMINATE" or fastr_test_status.status == "FAILED"):
            fastr_test_status.status = "OK"

        # write out a file with the test status for each output (that exists)
        with open(join(_pkg_testdir('fastr', pkg), 'testfile_status'), 'w') as f:
            f.write('# <file path> <tests passed> <tests skipped> <tests failed>\n')
            for fastr_relpath, fastr_testfile_status in fastr_outputs.iteritems():
                if fastr_testfile_status.status == "FAILED":
                    relpath = fastr_relpath + ".fail"
                else:
                    relpath = fastr_relpath

                test_output_file = join(_pkg_testdir('fastr', pkg), relpath)
                if os.path.exists(test_output_file):
                    print "Generating testfile_status for {0}".format(str(test_output_file))
                    #ok, skipped, failed = handle_output_file(test_output_file, fastr_testfile_status)
                    ok, skipped, failed = fastr_testfile_status.report
                    f.write("{0} {1} {2} {3}\n".format(relpath, ok, skipped, failed))

        print 'END checking ' + pkg


def handle_output_file(test_output_file_contents):
    """
    R package tests are usually distributed over several files. Each file can be interpreted as a test suite.
    This function parses the output file of all test suites and tries to detect if it used the testthat or RUnit.
    In this case, it parses the summary (number of passed, skipped, failed tests) of these test frameworks.
    If none of the frameworks is used, it performs an output diff and tries to determine, how many statements
    produces different output, i.e., every statement is considered to be a unit test.
    :param test_output_file_contents: the lines of the output file
    :return: A 3-tuple with the number of passed, skipped, and failed tests.
    """
    for i in range(0, len(test_output_file_contents)):
        if test_output_file_contents[i].startswith("testthat results"):
            return _parse_testthat_result(test_output_file_contents, i)

        # TODO parse RUnit test protocol

    # if this test did not use one of the known test frameworks, take the report from the fuzzy compare
    return None, None, None


def _parse_testthat_result(lines, i):
    '''
    OK: 2 SKIPPED: 0 FAILED: 0
    '''
    if i+1 < len(lines) and lines[i+1].startswith("OK"):
        result_line = lines[i+1]
        idx_ok = 0
        idx_skipped = result_line.find("SKIPPED")
        idx_failed = result_line.find("FAILED")
        if idx_ok != -1 and idx_skipped != -1 and idx_failed != -1:
            ok_part = result_line[idx_ok:idx_skipped]
            skipped_part = result_line[idx_skipped:idx_failed]
            failed_part = result_line[idx_failed:]
            return (_testthat_parse_part(ok_part), _testthat_parse_part(skipped_part), _testthat_parse_part(failed_part))
        raise Exception("Could not parse testthat status line {0}".format(result_line))

def _testthat_parse_part(part):
    '''
    parses a part like "OK: 2"
    '''
    parts = part.split(":")
    if len(parts) == 2:
        assert parts[0] == "OK" or parts[0] == "SKIPPED" or parts[0] == "FAILED"
        return int(parts[1])
    raise Exception("could not parse testthat status part {0}".format(part))


def _find_start(content):
    marker = "Type 'q()' to quit R."
    for i in range(len(content)):
        line = content[i]
        if marker in line:
            # skip blank lines
            j = i + 1
            while j < len(content):
                line = content[j].strip()
                if len(line) > 0:
                    return j
                j = j + 1
    return None


def _find_end(content):
    marker = "Time elapsed:"
    for i in range(len(content)):
        line = content[i]
        if marker in line:
            return i
    # not all files have a Time elapsed:
    return len(content)


def _find_line(gnur_line, fastr_content, fastr_i):
    '''
    Search forward in fastr_content from fastr_i searching for a match with gnur_line.
    Do not match empty lines!
    '''
    if gnur_line == '\n':
        return -1
    while fastr_i < len(fastr_content):
        fastr_line = fastr_content[fastr_i]
        if fastr_line == gnur_line:
            return fastr_i
        fastr_i = fastr_i + 1
    return -1


def _replace_engine_references(output):
    for idx, val in enumerate(output):
        if "RUNIT TEST PROTOCOL -- " in val:
            # RUnit prints the current date and time
            output[idx] = "RUNIT TEST PROTOCOL -- <date/time>"
        else:
            # ignore differences which come from test directory paths
            output[idx] = val.replace('fastr', '<engine>').replace('gnur', '<engine>')

def _fuzzy_compare(gnur_content, fastr_content, gnur_filename, fastr_filename):
    '''
    Compares the test output of GnuR and FastR by ignoring implementation-specific differences like header, error,
    and warning messages.
    It returns a 3-tuple (<status>, <statements passed>, <statements failed>), where status=0 if files are equal,
    status=1 if the files are different, status=-1 if the files could not be compared. In case of status=1,
    statements passed and statements failed give the numbers on how many statements produced the same or a different
    output, respectively.
    '''
    _replace_engine_references(gnur_content)
    _replace_engine_references(fastr_content)
    gnur_start = _find_start(gnur_content)
    gnur_end = _find_end(gnur_content)
    fastr_start = _find_start(fastr_content)
    fastr_len = len(fastr_content)
    if not gnur_start or not gnur_end or not fastr_start:
        return -1, 0, 0
    gnur_i = gnur_start
    fastr_i = fastr_start
    # the overall result for comparing the file
    overall_result = 0
    # the local result, i.e., for the current statement
    result = 0
    statements_passed = set()
    statements_failed = set()

    # the first line must start with the prompt, so capture it
    gnur_prompt = _capture_prompt(gnur_content, gnur_i)
    fastr_prompt = _capture_prompt(fastr_content, fastr_i)

    gnur_cur_statement_start = -1
    fastr_cur_statement_start = -1
    while gnur_i < gnur_end:
        gnur_line = gnur_content[gnur_i]
        if fastr_i >= fastr_len:
            overall_result = 1
            break

        fastr_line = fastr_content[fastr_i]

        # check if the current line starts a statement
        if _is_statement_begin(gnur_prompt, gnur_line) and gnur_cur_statement_start != gnur_i:
            gnur_cur_statement_start = gnur_i

        # if we find a new statement begin
        if _is_statement_begin(fastr_prompt, fastr_line) and fastr_cur_statement_start != fastr_i:
            fastr_cur_statement_start = fastr_i

        # flag indicating that we want to synchronize
        sync = False
        if gnur_line != fastr_line:
            if fastr_line.startswith('Warning') and 'FastR does not support graphics package' in fastr_content[fastr_i + 1]:
                # ignore warning about FastR not supporting the graphics package
                fastr_i = fastr_i + 2
                if fastr_content[fastr_i].startswith('NULL') and not gnur_line.startswith('NULL'):
                    # ignore additional visible NULL
                    fastr_i = fastr_i + 1
                continue
            if gnur_line.startswith('Warning') and gnur_i + 1 < gnur_end and 'closing unused connection' in gnur_content[gnur_i + 1]:
                # ignore message about closed connection
                gnur_i = gnur_i + 2
                continue
            if gnur_i > 0 and gnur_content[gnur_i - 1].startswith('   user  system elapsed'):
                # ignore differences in timing
                gnur_i = gnur_i + 1
                fastr_i = fastr_i + 1
                continue
            # we are fuzzy on Error/Warning as FastR often differs
            # in the context/format of the error/warning message AND GnuR is sometimes
            # inconsistent over which error message it uses. Unlike the unit test environment,
            # we cannot tag tests in any way, so we simply check that FastR does report
            # an error. We then scan forward to try to get the files back in sync, as the
            # the number of error/warning lines may differ.
            if 'Error' in gnur_line or 'Warning' in gnur_line:
                to_match = 'Error' if 'Error' in gnur_line else 'Warning'
                if to_match not in fastr_line:
                    result = 1
                    # XXX do not break
                    # break
                else:
                    # accept differences in the error/warning messages but we need to synchronize
                    sync = True

            else:
                # genuine difference (modulo whitespace)
                if not _ignore_whitespace(gnur_line, fastr_line):
                    result = 1
                    # XXX do not break, but we might need to synchronize indices
                    # break


        # report a mismatch or success
        if result == 1:
            # we need to synchronize the indices such that we can continue
            sync = True
            # report the last statement to produce different output
            assert fastr_cur_statement_start != -1
            if fastr_cur_statement_start in statements_passed:
                statements_passed.remove(fastr_cur_statement_start)
            statements_failed.add(fastr_cur_statement_start)
        else:
            assert result == 0
            if fastr_cur_statement_start not in statements_failed:
                statements_passed.add(fastr_cur_statement_start)

        # synchronize: skip until lines match (or file end reached)
        if sync:
            gnur_i = gnur_i + 1
            fastr_i = fastr_i + 1
            if gnur_i == gnur_end - 1:
                # at end (there is always a blank line)
                break
            ni = -1
            # find next statement line (i.e. starting with a prompt)


            while gnur_i < gnur_end:
                if _is_statement_begin(gnur_prompt, gnur_content[gnur_i]):
                    ni = _find_line(gnur_content[gnur_i], fastr_content, fastr_i)
                    if ni > 0:
                        break
                gnur_i = gnur_i + 1
            if ni > 0:
                fastr_i = ni

            overall_result = 1
            result = 0
        else:
            # just advance by one line in FastR and GnuR
            gnur_i = gnur_i + 1
            fastr_i = fastr_i + 1

    if overall_result == 1:
        print gnur_filename + ':%d' % gnur_i + ' vs. ' + fastr_filename + ':%d' % fastr_i
        print gnur_line.strip()
        print "vs."
        print fastr_line.strip()
    return overall_result, len(statements_passed), len(statements_failed)


def _ignore_whitespace(gnur_line, fastr_line):
    return gnur_line.translate(None, ' \t') == fastr_line.translate(None, ' \t')


def _capture_prompt(lines, idx):
    # The prompt can be anything, so it is hard to determine it in general.
    # We will therefore just consider the default prompt.
    DEFAULT_PROMPT = "> "
    if idx < len(lines) and lines[idx].startswith(DEFAULT_PROMPT):
        return DEFAULT_PROMPT
    return None


def _is_statement_begin(captured_prompt, line):
    line_wo_prompt = line.replace(captured_prompt, "").strip()
    return line.startswith(captured_prompt) and line_wo_prompt is not "" and not line_wo_prompt.startswith("#")


def pkgtest_cmp(args):
    with open(args[0]) as f:
        gnur_content = f.readlines()
    with open(args[1]) as f:
        fastr_content = f.readlines()
    return _fuzzy_compare(gnur_content, fastr_content, args[0], args[1])


def find_top100(args):
    libinstall = join(_fastr_suite_dir(), "top100.tmp")
    if not os.path.exists(libinstall):
        os.mkdir(libinstall)
    os.environ['R_LIBS_USER'] = libinstall
    _installpkgs(['--find-top100', '--use-installed-pkgs'])


def remove_dup_pkgs(args):
    pkgs = args[0].split(",")
    x = dict()
    for p in pkgs:
        x[p] = 1
    result = []
    for p in x.iterkeys():
        result += p
    return result


def computeApiChecksum(includeDir):
    '''
    Computes a checksum of the header files found in the provided directory (recursively).
    The result is a SHA256 checksum (as string with hex digits) of all header files.
    '''
    m = hashlib.sha256()
    rootDir = includeDir
    for root, _, files in os.walk(rootDir):
        mx.logvv("Visiting directory {0}".format(root))
        for f in files:
            fileName = join(root, f)
            if fileName.endswith('.h'):
                try:
                    mx.logvv("Including file {0}".format(fileName))
                    with open(fileName) as f:
                        m.update(f.read())
                except IOError as e:
                    # Ignore errors on broken symlinks
                    if not os.path.islink(fileName) or os.path.exists(fileName):
                        raise e


    hxdigest = m.hexdigest()
    mx.logv("Computed API version checksum {0}".format(hxdigest))
    return hxdigest
