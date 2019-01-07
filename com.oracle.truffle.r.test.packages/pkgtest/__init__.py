#
# Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
import logging

quiet = False
verbose = False
dump_preprocessed = False
graalvm = None
__fastr_home = None
__gnur_home = None


def abort(status, *args):
    if args:
        logging.error(*args)
    quit(status)


def _fastr_suite_dir():
    return __fastr_home


def get_gnur_home():
    '''
    Returns path to GnuR home dir, e.g., gnur/gnur/R-3.4.0/.
    '''
    return __gnur_home


def _gnur_rscript():
    '''
    returns path to Rscript in sibling gnur directory
    '''
    # return _mx_gnur().extensions._gnur_rscript_path()
    return join(get_gnur_home(), "bin", "Rscript")


def _gnur_include_path():
    # if _graalvm():
    #     return join(_mx_gnur().dir, 'gnur', _mx_gnur().extensions.r_version(), 'include')
    # return join(mx_fastr._gnur_path(), "include")
    return join(get_gnur_home(), 'include')


def _fastr_include_path():
    if _graalvm():
        return join(graalvm, "jre", "languages", "R", "include")
    return join(_fastr_suite_dir(), 'include')


def _graalvm_rscript():
    assert graalvm is not None
    return join(graalvm, 'bin', 'Rscript')


def _fastr_rscript():
    graalvm_dir = _graalvm()
    if graalvm_dir is not None:
        return join(graalvm_dir, "bin", "Rscript")
    return join(_fastr_suite_dir(), 'bin', 'Rscript')


def _get_r_version(rscript_binary):
    args = ["--silent", "-e", "cat(R.Version()[['major']], '.', R.Version()[['minor']], '\\n', sep='')"]
    return subprocess.check_output([rscript_binary] + args, stderr=subprocess.STDOUT).rstrip()


def _graalvm():
    return graalvm


def _check_r_versions():
    '''
    Checks that FastR and GnuR have the same version.
    '''
    gnur_version = _get_r_version(_gnur_rscript())
    fastr_version = _get_r_version(_fastr_rscript())
    logging.info("Using FastR version = %s ; GnuR version = %s: " % (fastr_version, gnur_version))
    if gnur_version != fastr_version:
        abort(1, 'graalvm R version does not match gnur suite: %s (GnuR) vs. %s (FastR)' % (gnur_version, fastr_version))


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
        print ("timestamp: {0}".format(str(datetime.now())))


def _find_subdir(root, name, fatalIfMissing=True):
    for dirpath, dnames, _ in os.walk(root):
        for f in dnames:
            if f == name:
                return os.path.join(dirpath, f)
    if fatalIfMissing:
        raise Exception(name)


def _log_step(state, step, rvariant):
    if not quiet:
        print ("{0} {1} with {2}".format(state, step, rvariant))
        _log_timestamp()


def _packages_test_project():
    return 'com.oracle.truffle.r.test.packages'


def _packages_test_project_dir():
    return _find_subdir(_fastr_suite_dir(), _packages_test_project())


def _ensure_R_on_PATH(env, bindir):
    '''
    Some packages (e.g. stringi) require that 'R' is actually on the PATH
    '''
    env['PATH'] = join(bindir) + os.pathsep + os.environ['PATH']


def _installpkgs_script():
    packages_test = _packages_test_project_dir()
    return join(packages_test, 'r', 'install.packages.R')


def installpkgs(args, **kwargs):
    '''
    Runs the R script that does package/installation and testing.
    '''
    if kwargs.has_key('env'):
        env = kwargs['env']
    else:
        env = os.environ.copy()
        kwargs['env'] = env

    if "FASTR_WORKING_DIR" in os.environ:
        env["TMPDIR"] = os.environ["FASTR_WORKING_DIR"]

    script = _installpkgs_script()
    logging.debug("Using FastR binary: " + _fastr_rscript())
    _ensure_R_on_PATH(env, os.path.dirname(_fastr_rscript()))
    process = subprocess.Popen([_fastr_rscript(), script] + args, env=env)
    process.wait()
    return process.returncode


def prepare_r_install_arguments(args):
    # install and test the packages, unless just listing versions
    if not '--list-versions' in args:
        args += ['--run-tests']
        args += ['--testdir', 'test.fastr']
        if not '--print-install-status' in args:
            args += ['--print-install-status']
    return args


def pkgtest(args):
    '''
    Package installation/testing.

    Options:
        --quiet                  Reduce output during testing.
        --cache-pkgs dir=DIR     Use package cache in directory DIR (will be created if not existing).
                                 Optional parameters:
                                     size=N        Maximum number of different API versions in the cache.
        --no-install             Do not install any packages (can only test installed packages).
        --list-versions          List packages to be installed/tested without installing/testing them.
        --pkg-pattern PATTERN    A regular expression to match packages.
        --verbose, -v            Verbose output.
        --very-verbose, -V       Very verbose output.
        --dump-preprocessed      Dump the preprocessed output files to the current working directory.

    Return codes:
        0: success
        1: install fail
        2: test fail
        3: install & test fail
    '''
    unknown_args = parse_arguments(args)
    install_args = prepare_r_install_arguments(unknown_args)

    test_installed = '--no-install' in install_args
    fastr_libinstall, fastr_install_tmp = _create_libinstall('fastr', test_installed)
    gnur_libinstall, gnur_install_tmp = _create_libinstall('gnur', test_installed)

    install_args = list(install_args)

    env = os.environ.copy()
    env["TMPDIR"] = fastr_install_tmp
    env['R_LIBS_USER'] = fastr_libinstall
    env['FASTR_OPTION_PrintErrorStacktracesToFile'] = 'false'
    env['FASTR_OPTION_PrintErrorStacktraces'] = 'true'

    # If '--cache-pkgs' is set, then also set the native API version value
    _set_pkg_cache_api_version(install_args, _fastr_include_path())

    _log_step('BEGIN', 'install/test', 'FastR')
    # Currently installpkgs does not set a return code (in install.packages.R)
    out = OutputCapture()
    rc = installpkgs(install_args, nonZeroIsFatal=False, env=env, out=out, err=out)
    if rc == 100:
        # fatal error connecting to package repo
        abort(status=rc)

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
        print ('Test Status')
        for pkg, test_status in out.test_info.iteritems():
            if test_status.status != "OK":
                rc = rc | 2
            print ('{0}: {1}'.format(pkg, test_status.status))

        diffdir = _create_testdot('diffs')
        for pkg, _ in out.test_info.iteritems():
            diff_file = join(diffdir, pkg)
            subprocess.call(['diff', '-r', _pkg_testdir('fastr', pkg), _pkg_testdir('gnur', pkg)],
                            stdout=open(diff_file, 'w'))

    shutil.rmtree(fastr_install_tmp, ignore_errors=True)
    return rc


def _set_pkg_cache_api_version(arg_list, include_dir):
    '''
    Looks for argument '--cache-pkgs' and appends the native API version to the value list of this argument.
    '''
    if "--cache-pkgs" in arg_list:
        pkg_cache_values_idx = arg_list.index("--cache-pkgs") + 1
        if pkg_cache_values_idx < len(arg_list):
            if 'version=' in arg_list[pkg_cache_values_idx]:
                logging.info("Ignoring specified API version and using automatically computed one.")
            arg_list[pkg_cache_values_idx] = arg_list[pkg_cache_values_idx] + ",version={0}".format(
                computeApiChecksum(include_dir))


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
        print (data)
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

    # forward any explicit args to pkgtest
    args = [_installpkgs_script()]
    args += forwarded_args
    args += ['--pkg-filelist', gnur_packages]
    args += ['--run-tests']
    args += ['--ignore-blacklist']
    args += ['--testdir', 'test.gnur']
    _log_step('BEGIN', 'install/test', 'GnuR')

    logging.debug("Using GnuR binary: " + _gnur_rscript())
    _ensure_R_on_PATH(env, os.path.dirname(_gnur_rscript()))
    subprocess.Popen([_gnur_rscript()] + args, env=env)

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
        logging.info('BEGIN checking ' + pkg)
        gnur_test_status = gnur_test_info[pkg]
        fastr_test_status = fastr_test_info[pkg]
        gnur_outputs = gnur_test_status.testfile_outputs
        fastr_outputs = fastr_test_status.testfile_outputs
        if _failed_outputs(gnur_outputs):
            # What this likely means is that some native package is not
            # installed on the system so GNUR can't run the tests.
            # Ideally this never happens.
            logging.info("{0}: GnuR test had .fail outputs".format(pkg))

        if _failed_outputs(fastr_outputs):
            # In addition to the similar comment for GNU R, this can happen
            # if, say, the JVM crashes (possible with native code packages)
            logging.info("{0}: FastR test had .fail outputs".format(pkg))
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
                logging.info("{0}: FastR is missing output file: {1}".format(pkg, gnur_test_output_relpath))
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

            # parse custom filters from file
            filters = _select_filters(
                _parse_filter_file(os.path.join(_packages_test_project_dir(), "test.output.filter")), pkg)

            # first, parse file and see if a known test framework has been used
            detected, ok, skipped, failed = handle_output_file(fastr_testfile_status.abspath, fastr_content)
            if detected:
                # If a test framework is used, also parse the summary generated by GnuR to compare numbers.
                detected, gnur_ok, gnur_skipped, gnur_failed = handle_output_file(gnur_testfile_status.abspath,
                                                                                  gnur_content)
                fastr_invalid_numbers = ok is None or skipped is None and failed is None
                gnur_invalid_numbers = gnur_ok is None or gnur_skipped is None and gnur_failed is None
                total_fastr = ok + skipped + failed if not fastr_invalid_numbers else -1
                total_gnur = gnur_ok + gnur_skipped + gnur_failed if not gnur_invalid_numbers else -1

                if not fastr_invalid_numbers and total_fastr != total_gnur:
                    logging.info(
                        "Different number of tests executed. FastR = {} vs. GnuR = {}".format(total_fastr, total_gnur))
                elif fastr_invalid_numbers:
                    logging.info("FastR reported invalid numbers of executed tests.")

                if fastr_invalid_numbers or total_fastr > total_gnur:
                    # If FastR's numbers are invalid or GnuR ran fewer tests than FastR, we cannot trust the FastR numbers
                    fastr_testfile_status.report = 0, gnur_skipped, gnur_ok + gnur_failed
                    fastr_test_status.status = "FAILED"
                    fastr_testfile_status.status = "FAILED"
                elif total_fastr < total_gnur:
                    # If FastR ran fewer tests than GnuR, we complement the missing ones as failing
                    fastr_testfile_status.report = ok, skipped, failed + (total_gnur - total_fastr)
                    fastr_test_status.status = "FAILED"
                    fastr_testfile_status.status = "FAILED"
                else:
                    # The total numbers are equal, so we are fine.
                    fastr_testfile_status.status = "OK"
                    fastr_testfile_status.report = ok, skipped, failed
            else:
                from fuzzy_compare import fuzzy_compare
                result, n_tests_passed, n_tests_failed = fuzzy_compare(gnur_content, fastr_content,
                                                                        gnur_testfile_status.abspath,
                                                                        fastr_testfile_status.abspath,
                                                                        custom_filters=filters)
                if result == -1:
                    logging.info("{0}: content malformed: {1}".format(pkg, gnur_test_output_relpath))
                    fastr_test_status.status = "INDETERMINATE"
                    # we don't know how many tests are in there, so consider the whole file to be one big skipped test
                    fastr_testfile_status.report = 0, 1, 0
                    # break
                elif result != 0:
                    fastr_test_status.status = "FAILED"
                    fastr_testfile_status.status = "FAILED"
                    fastr_testfile_status.report = n_tests_passed, 0, n_tests_failed
                    logging.info("{0}: FastR output mismatch: {1}".format(pkg, gnur_test_output_relpath))
                    # break
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
                logging.info("generating testfile_status for {0}".format(fastr_relpath))
                relpath = fastr_relpath
                test_output_file = join(_pkg_testdir('fastr', pkg), relpath)

                if os.path.exists(test_output_file):
                    ok, skipped, failed = fastr_testfile_status.report
                    f.write("{0} {1} {2} {3}\n".format(relpath, ok, skipped, failed))
                elif fastr_testfile_status.status == "FAILED":
                    # In case of status == "FAILED", also try suffix ".fail" because we just do not know if the test
                    # failed and finished or just never finished.
                    relpath_fail = fastr_relpath + ".fail"
                    test_output_file_fail = join(_pkg_testdir('fastr', pkg), relpath_fail)
                    if os.path.exists(test_output_file_fail):
                        ok, skipped, failed = fastr_testfile_status.report
                        f.write("{0} {1} {2} {3}\n".format(relpath_fail, ok, skipped, failed))
                    else:
                        logging.info("File {0} or {1} does not exist".format(test_output_file, test_output_file_fail))
                else:
                    logging.info("File {0} does not exist".format(test_output_file))

        logging.info('END checking ' + pkg)


def handle_output_file(test_output_file, test_output_file_lines):
    """
    R package tests are usually distributed over several files. Each file can be interpreted as a test suite.
    This function parses the output file of all test suites and tries to detect if it used the testthat or RUnit.
    In this case, it parses the summary (number of passed, skipped, failed tests) of these test frameworks.
    If none of the frameworks is used, it performs an output diff and tries to determine, how many statements
    produces different output, i.e., every statement is considered to be a unit test.
    Returns a 4-tuple: (<framework detected>, <#passed>, <#skipped>, <#failed>).
    """
    logging.debug("Detecting output type of {!s}".format(test_output_file))
    detected = False
    ok, skipped, failed = None, None, None
    try:
        if _is_testthat_result(test_output_file):
            # if "testthat results" in test_output_file_contents[i]:
            logging.info("Detected testthat summary in {!s}".format(test_output_file))
            detected = True
            ok, skipped, failed = _parse_testthat_result(test_output_file_lines)
        elif _is_runit_result(test_output_file_lines):
            logging.info("Detected RUNIT test protocol in {!s}".format(test_output_file))
            detected = True
            ok, skipped, failed = _parse_runit_result(test_output_file_lines)
    except TestFrameworkResultException as e:
        logging.info("Error parsing test framework summary: " + str(e))
    # if this test did not use one of the known test frameworks, take the report from the fuzzy compare
    return (detected, ok, skipped, failed)


def _is_testthat_result(test_output_file):
    return os.path.basename(test_output_file) == "testthat.Rout"


def _is_runit_result(lines):
    return any("RUNIT TEST PROTOCOL" in l for l in lines)


def _parse_testthat_result(lines):
    '''
    OK: 2 SKIPPED: 0 FAILED: 0
    '''

    def _testthat_parse_part(part):
        '''
        parses a part like "OK: 2"
        '''
        parts = part.split(":")
        if len(parts) == 2:
            assert parts[0] == "OK" or parts[0] == "SKIPPED" or parts[0] == "FAILED"
            return int(parts[1])
        raise Exception("could not parse testthat status part {0}".format(part))

    # find index of line which contains 'testthat results'
    try:
        i = next(iter([x for x in enumerate(lines) if 'testthat results' in x[1]]))[0]
        if i + 1 < len(lines) and lines[i + 1].startswith("OK"):
            result_line = lines[i + 1]
            idx_ok = 0
            idx_skipped = result_line.find("SKIPPED")
            idx_failed = result_line.find("FAILED")
            if idx_ok != -1 and idx_skipped != -1 and idx_failed != -1:
                ok_part = result_line[idx_ok:idx_skipped]
                skipped_part = result_line[idx_skipped:idx_failed]
                failed_part = result_line[idx_failed:]
                return (
                    _testthat_parse_part(ok_part), _testthat_parse_part(skipped_part),
                    _testthat_parse_part(failed_part))
            raise TestFrameworkResultException("Could not parse testthat status line {0}".format(result_line))
        else:
            raise TestFrameworkResultException("Could not parse testthat summary at line {}".format(i + 1))
    except StopIteration:
        raise TestFrameworkResultException("Could not parse testthat summary: Line 'testthat results' not contained.")


def _parse_runit_result(lines):
    '''
    RUNIT TEST PROTOCOL -- Thu Feb 08 10:54:42 2018
    ***********************************************
    Number of test functions: 20
    Number of errors: 0
    Number of failures: 0
    '''
    try:
        line_idx = next(iter([x for x in enumerate(lines) if 'RUNIT TEST PROTOCOL' in x[1]]))[0]
        tests_total = 0
        tests_failed = 0
        for i in range(line_idx, len(lines)):
            split_line = lines[i].split(":")
            if len(split_line) >= 2:
                if "Number of test functions" in split_line[0]:
                    tests_total = int(split_line[1])
                elif "Number of errors" in split_line[0] or "Number of failures" in split_line[0]:
                    tests_failed = tests_failed + int(split_line[1])
        return (tests_total - tests_failed, 0, tests_failed)
    except StopIteration:
        # That should really not happen since RUnit is detected by a line containing 'RUNIT TEST PROTOCOL'
        raise TestFrameworkResultException(
            "Could not parse testthat summary: Line 'RUNIT TEST PROTOCOL' not contained.")


def pkgtest_cmp(args):
    with open(args[0]) as f:
        gnur_content = f.readlines()
    with open(args[1]) as f:
        fastr_content = f.readlines()
    from fuzzy_compare import fuzzy_compare
    return fuzzy_compare(gnur_content, fastr_content, args[0], args[1])


def find_top100():
    find_top(["100"])


def find_top(args):
    n = args[-1]
    libinstall = join(_fastr_suite_dir(), "top%s.tmp" % n)
    if not os.path.exists(libinstall):
        os.mkdir(libinstall)
    os.environ['R_LIBS_USER'] = libinstall
    installpkgs(['--use-installed-pkgs', '--find-top', n])


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
    """
    Computes a checksum of the header files found in the provided directory (recursively).
    The result is a SHA256 checksum (as string with hex digits) of all header files.
    """
    m = hashlib.sha256()
    rootDir = includeDir
    fileList = list()
    for root, _, files in os.walk(rootDir):
        logging.debug("Visiting directory %s" % root)
        for f in files:
            fileName = join(root, f)
            if fileName.endswith('.h'):
                logging.debug("Including file %s" % fileName)
                fileList.append(fileName)

    # sorting makes the checksum independent of the FS traversal order
    fileList.sort()
    for fileName in fileList:
        try:
            with open(fileName) as f:
                m.update(f.read())
        except IOError as e:
            # Ignore errors on broken symlinks
            if not os.path.islink(fileName) or os.path.exists(fileName):
                raise e

    hxdigest = m.hexdigest()
    logging.debug("Computed API version checksum {0}".format(hxdigest))
    return hxdigest


class TestFrameworkResultException(BaseException):
    pass


def parse_arguments(argv):
    """
    Parses the given argument vector and stores the values of the arguments known by this script to appropriate globals.
    The unknown arguments are returned for further processing.
    """
    import argparse
    parser = argparse.ArgumentParser(description='FastR package testing.')
    parser.add_argument('--fastr-home', metavar='FASTR_HOME', dest="fastr_home", type=str, default=None,
                        required=True, help='The FastR standalone repo home directory.')
    parser.add_argument('--gnur-home', metavar="GNUR_HOME", dest="gnur_home", default=None, required=True,
                        help='The GnuR home directory.')
    parser.add_argument('--graalvm-home', metavar="GRAALVM_HOME", dest="graalvm_home", default=None,
                        help='The GraalVM root directory.')
    parser.add_argument('-v', '--verbose', dest="verbose", action="store_const", const=1, default=0,
                        help='Do verbose logging.')
    parser.add_argument('-V', '--very-verbose', dest="verbose", action="store_const", const=2,
                        help='Do verbose logging.')
    parser.add_argument('--dump-preprocessed', dest="dump-preprocessed", action="store_true",
                        help='Dump processed output files where replacement filters have been applied.')
    parser.add_argument('-q', '--quiet', dest="quiet", type=bool, default=False,
                        help='Do verbose logging.')
    known_args, r_args = parser.parse_known_args(args=argv)

    global verbose, quiet, dump_preprocessed, __fastr_home, __gnur_home, graalvm
    __fastr_home = known_args.fastr_home
    __gnur_home = known_args.gnur_home
    graalvm = known_args.graalvm_home

    verbose = known_args.verbose
    quiet = known_args.quiet

    logging.debug("known_args: %s" % known_args)

    # print info if GraalVM is used
    if _graalvm():
        logging.info("Using GraalVM at %r" % _graalvm())

    # ensure that FastR and GnuR have the same version
    _check_r_versions()

    return r_args


if __name__ == "__main__":
    # run install/test
    import sys
    pkgtest(sys.argv)