#
# Copyright (c) 2015, 2015, Oracle and/or its affiliates. All rights reserved.
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

import os, sys, urllib, re
import subprocess
import mx
from os.path import join
from argparse import ArgumentParser
from HTMLParser import HTMLParser
from datetime import datetime

def _gather_test_outputs_forpkg(pkgdirpath):
    '''return a sorted list of paths to .Rout/.fail files in pkgdirpath'''
    result = []
    for dirpath, _, files in os.walk(pkgdirpath):
        for f in files:
            if f.endswith(".Rout") or f.endswith(".fail"):
                result.append(join(dirpath, f))
    result.sort()
    return result

def _gather_test_outputs(testdir):
    '''return a dict mapping package names to sorted list of output file paths'''
    result = dict()
    for dirpath, dirs, _ in os.walk(testdir):
        for d in dirs:
            result[d] = _gather_test_outputs_forpkg(join(dirpath, d))
        # only interested in top level
        break
    return result

def _gather_all_test_outputs(testdir, all_results=False):
    '''
    Builds a dict mapping package names to a sorted list of ResultInfo updated with map from package name
    to  list of output file paths.
    if all=False, only the most recent ResultInfo is in the list, otherwise all
    '''
    fastr = dict()
    fastr_date = dict() # holds most recent resultInfo.date
    dirlist = get_local_dirs(testdir)
    for resultdir in dirlist:
        resultInfo = ResultInfo(resultdir)
        result_outputs = _gather_test_outputs(join(testdir, resultdir, "test"))
        for pkg, outputs in result_outputs.iteritems():
            if not fastr.has_key(pkg):
                resultInfo_list = []
                fastr[pkg] = resultInfo_list
            else:
                resultInfo_list = fastr[pkg]

            resultInfo.set_test_outputs(pkg, outputs)
            if all_results:
                resultInfo_list.append(resultInfo)
                fastr[pkg] = sorted(resultInfo_list)
            else:
                # if this is a more recent result overwrite, else drop
                if len(resultInfo_list) == 0 or resultInfo.date > resultInfo_list[0].date:
                    resultInfo_list = [resultInfo]
                    fastr[pkg] = resultInfo_list
                    fastr_date[pkg] = resultInfo.date
    return fastr

def _get_test_outputs(resultInfo_map, index=0, specific_pkg=None):
    '''
    Takes a map created by _gather_all_test_outputs and returns a new map,
    also keyed by pkg, to the list of test output files
    '''
    result = dict()
    if specific_pkg:
        result[specific_pkg] = resultInfo_map[specific_pkg][index].test_outputs[specific_pkg]
    else:
        for pkg, resultInfo_list in resultInfo_map.iteritems():
            if specific_pkg and specific_pkg != pkg:
                continue
            if index < len(resultInfo_list):
                result[pkg] = resultInfo_list[index].test_outputs[pkg]
            else:
                result[pkg] = []
    return result

def _find_start(content):
    marker = "Type 'q()' to quit R."
    for i in range(len(content)):
        line = content[i]
        if marker in line:
            return i + 1

def _find_end(content):
    marker = "Time elapsed:"
    for i in range(len(content)):
        line = content[i]
        if marker in line:
            return i - 1

def _fuzzy_compare(gnur_content, fastr_content):
    gnur_start = _find_start(gnur_content) + 1 # Gnu has extra empty line
    gnur_end = _find_end(gnur_content)
    fastr_start = _find_start(fastr_content)
    fastr_len = len(fastr_content)
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

def rpt_list_testdates(args):
    parser = ArgumentParser(prog='mx rpt-list-testdates')
    _add_common_args(parser)
    parser.add_argument('--printdir', action='store_true', help='print directory containing tests')
    _add_pattern_arg(parser)
    args = parser.parse_args(args)
    fastr = dict()
    local_dirs = get_local_dirs(args.logdir)
    for local_dir in local_dirs:
        resultInfo = ResultInfo(local_dir)
        result_outputs = _gather_test_outputs(join(args.logdir, local_dir, "test"))
        for pkg, _ in result_outputs.iteritems():
            if re.search(args.pattern, pkg) is None:
                continue
            if not fastr.has_key(pkg):
                testdates = []
                fastr[pkg] = testdates
            else:
                testdates = fastr[pkg]
            testdates.append(resultInfo)

    for pkg, testdates in fastr.iteritems():
        sortedList = sorted(testdates, reverse=True)
        print pkg
        for resultInfo in sortedList:
            if args.printdir:
                print '  ' + join(args.logdir, resultInfo.localdir)
            else:
                print '  ' + str(resultInfo.date)

def rpt_compare(args):
    '''
    Analyze package test results by comparing test output with GnuR output.
    Uses either a specific directory, i.e. the 'test' subdirectory of the --testdir argument
    or (default) the latest downloaded results from the --logdir directory
    Return 0 if passed, non-zero if failed
    '''
    parser = ArgumentParser(prog='mx rpt-compare')
    _add_common_args(parser)
    parser.add_argument('--testdir', action='store', help='specific dir containing fastr results')
    parser.add_argument('--pkg', action='store', help='pkg to compare')
    parser.add_argument('--diff', action='store_true', help='execute given diff program on differing outputs')
    parser.add_argument('--difftool', action='store', help='diff tool', default='diff')
    _add_pattern_arg(parser)
    args = parser.parse_args(args)

    if args.pkg:
        # backwards compatibility
        args.pattern = args.pkg

    gnur = _gather_test_outputs(join(os.getcwd(), "test_gnur"))

    if args.testdir:
        fastr = _gather_test_outputs(join(args.testdir, "test"))
    else:
        fastr = _get_test_outputs(_gather_all_test_outputs(args.logdir))

    rdict = _rpt_compare_pkgs(gnur, fastr, args.verbose, args.pattern, args.diff, args.difftool)
    for _, rc in rdict.iteritems():
        if rc == 1:
            return 1
    return 0

def _rpt_compare_pkgs(gnur, fastr, verbose, pattern, diff=False, difftool=None):
    '''
    returns dict keyed by pkg with value 0 for pass, 1 for fail
    '''
    # gnur is definitive
    result = dict()
    for pkg in fastr.keys():
        if re.search(pattern, pkg) is None:
            continue
        if not gnur.has_key(pkg):
            print 'no gnur output to compare: ' + pkg
            continue

        result[pkg] = 0 # optimistic
        if verbose:
            print 'comparing ' + pkg
        fastr_outputs = fastr[pkg]
        gnur_outputs = gnur[pkg]
        if len(fastr_outputs) != len(gnur_outputs):
            if verbose:
                print 'fastr is missing some output files'
                # TODO continue but handle missing files in loop?
                # does it ever happen in practice?
            result[pkg] = 1
            continue
        for i in range(len(gnur_outputs)):
            fastr_output = fastr_outputs[i]
            gnur_output = gnur_outputs[i]
            gnur_content = None
            with open(gnur_output) as f:
                gnur_content = f.readlines()
            fastr_content = None
            with open(fastr_output) as f:
                fastr_content = f.readlines()
            result[pkg] = _fuzzy_compare(gnur_content, fastr_content)
            if result[pkg] != 0:
                if verbose:
                    print 'mismatch on file: ' + fastr_output
                if diff:
                    cmd = [difftool, gnur_output, fastr_output]
                    print ' '.join(cmd)
                    subprocess.call(cmd)
                break
    return result

def check_install(result, text):
    lines = text.split("\n")
    nlines = len(lines)
    result_data = ""

    def find_done(index, pkgname):
        exception = False
        for i in range(index, nlines):
            line = lines[i]
            if 'exception' in line:
                exception = True
            if line.startswith("* DONE"):
                done_pkgname = line[line.find("(") + 1 : line.rfind(")")]
                return pkgname == done_pkgname and not exception
        return False

    start_index = None
    for i in range(nlines):
        line = lines[i]
        if line.startswith("BEGIN package installation"):
            start_index = i
            break

    if start_index is not None:
        for i in range(start_index, nlines):
            line = lines[i]
            if line.startswith("* installing *source* package"):
                pkgname = _extract_pkgname(line)
                result_data += pkgname
                if find_done(i, pkgname):
                    result_data += ": OK\n"
                else:
                    result_data += ": FAILED\n"
                    result = 1

    return result, result_data

def rpt_check_install_log(args):
    parser = ArgumentParser(prog='mx rpt-check-install-log')
    parser.add_argument('--log', action='store', help='file containing install log', required=True)
    args = parser.parse_args(args)

    with open(args.log) as f:
        content = f.read()
        _, result_data = check_install(0, content)

    print result_data

def _extract_pkgname(line):
    sx = line.find("'")
    ex = line.rfind("'")
    if sx < 0:
        sx = line.find("\xe2") + 2
        ex = line.rfind("\xe2")
    pkgname = line[sx + 1 : ex]
    return pkgname

class DirHTMLParser(HTMLParser):
    def __init__(self, files):
        HTMLParser.__init__(self)
        self.files = files

    def handle_starttag(self, tag, attrs):
        if tag == 'a':
            for attr in attrs:
                if attr[0] == 'href':
                    name = attr[1]
                    self.files.append(name)

class ResultInfo:
    def __init__(self, localdir):
        self.localdir = localdir
        date = localdir[7:33]
        cid = localdir[34:74]
        self.date = datetime.strptime(date, "%Y-%m-%d_%H:%M:%S.%f")
        self.cid = cid
        self.test_outputs = dict()

    def __str__(self):
        return "date: {0}, id {1}".format(self.date, self.cid)

    def __sortkey__(self):
        return self.date

    def set_test_outputs(self, pkg, outputs):
        self.test_outputs[pkg] = outputs

class Result:
    def __init__(self, resultInfo, content, rawData=None):
        self.resultInfo = resultInfo
        self.content = content.split('\n')
        self.rawData = rawData

    def __str__(self):
        return str(self.resultInfo)

class PkgStatus:
    def __init__(self, resultInfo, status):
        self.resultInfo = resultInfo
        self.status = status

    def __str__(self):
        return "{0}: {1}".format(str(self.resultInfo), self.status)

gate_url = 'http://diy-3-16/test_logs/fastr/'

class MatchClass:
    def __init__(self, print_matches, list_file_matches, match_string):
        self.print_matches = print_matches
        self.list_file_matches = list_file_matches
        self.match_string = match_string

    def record(self, pkgname, line):
        pass

    def output(self):
        pass

class DefaultMatchClass(MatchClass):
    def __init__(self, print_matches, list_file_matches, match_string):
        MatchClass.__init__(self, print_matches, list_file_matches, match_string)
        self.pkgdict = dict()

    def record(self, pkgname, line, filename):
        linelist = self.pkgdict.get(pkgname)
        if not linelist:
            linelist = []
            self.pkgdict[pkgname] = linelist
        linelist.append((line, filename))

    def output(self):
        print 'pkgs matching: ' + self.match_string
        for pkgname, linelist in self.pkgdict.iteritems():
            print pkgname
            if self.print_matches:
                for line_file in linelist:
                    print line_file[0]
            if self.list_file_matches:
                for line_file in linelist:
                    print line_file[1]


def find_matches(results, match_string, print_matches, list_file_matches, match_klass_name):
    if match_klass_name:
        mod = sys.modules[__name__]
        match_klass = getattr(mod, match_klass_name)
        if not match_klass:
            mx.abort('no such function: ' + match_klass_name)
        match_klass_instance = match_klass(print_matches, list_file_matches, match_string)
    else:
        match_klass_instance = DefaultMatchClass(print_matches, list_file_matches, match_string)

    for result in results:
        lines = result.rawData.split("\n")
        i = 0
        for line in lines:
            if match_string in line:
                # search backward for package install trace
                j = i
                pkgname = None
                while j > 0:
                    linej = lines[j]
                    if 'installing *source* package' in linej:
                        pkgname = _extract_pkgname(line)
                        match_klass_instance.record(pkgname, line, str(result))
                        break
                    j = j - 1
                if pkgname is None:
                    print 'failed to find installing trace starting at line: ' + str(i)
            i = i + 1
    match_klass_instance.output()

def _get_results(logdir):
    results = []
    localdirs = get_local_dirs(logdir)
    for localdir in localdirs:
        if 'result' in localdir:
            with open(os.path.join(logdir, localdir, 'testlog')) as f:
                rawData = f.read()
                result_data = check_install(0, rawData)[1]
                results.append(Result(ResultInfo(localdir), result_data, rawData))
    return results

def _build_pkgtable(results):
    # process files and build pkgtable
    pkgtable = dict()
    for result in results:
        for line in result.content:
            if len(line) == 0:
                continue
            pkg_status = line.split(':')
            if len(pkg_status) != 2:
                print 'ignoring ' + line
                continue
            pkgname = pkg_status[0]
            if not pkgname in pkgtable:
                pkgtable[pkgname] = []
            pkgtable[pkgname].append(PkgStatus(result.resultInfo, pkg_status[1].lstrip()))

    # sort occurrences by result date
    for _, occurrences in pkgtable.iteritems():
        if len(occurrences) > 1:
            occurrences.sort(key=lambda pkgstatus: pkgstatus.resultInfo.date, reverse=True)
    return pkgtable

def _add_common_args(parser):
    parser.add_argument("--logdir", action='store', help='directory of complete log files', default='install.cran.logs')
    parser.add_argument("--verbose", action='store_true', help='verbose output')

def _add_pattern_arg(parser):
    parser.add_argument('pattern', help='regexp pattern for pkg match', nargs='?', default='.*')

def get_gate_dirs(url, matchfun, adjustfun=None):
    gatedirlist = []
    urlf = urllib.urlopen(url)
    text = urlf.read()
    parser = DirHTMLParser(gatedirlist)
    parser.feed(text)
    urlf.close()
# pylint: disable=W0141
    gatedirlist = filter(matchfun, gatedirlist)
    filelist = []
    for gatedir in gatedirlist:
        if adjustfun:
            gatedir = adjustfun(gatedir)
        filelist.append(gatedir)
    return filelist

def get_local_dirs(logdir):
    filelist = []
    localdirs = os.listdir(logdir)
    for localdir in localdirs:
        if localdir.startswith('res'):
            filelist.append(localdir)
    return filelist

def _is_result_dir(d):
    return d.startswith('./result')

def _is_package_dir(d):
    return re.match("^[A-za-z]$", d[0]) is not None

def _strip_dotslash(d):
    l = len(d)
    return d[2:l - 1]

def _strip_slash(d):
    l = len(d)
    return d[0:l - 1]

def rpt_listnew(args):
    parser = ArgumentParser(prog='mx rpt-listnew')
    _add_common_args(parser)
    args = parser.parse_args(args)

    localdirs = get_local_dirs(args.logdir)
    gatedirs = get_gate_dirs(gate_url, _is_result_dir, _strip_dotslash)
    print "New gate files"
    for gatedir in gatedirs:
        if not gatedir in localdirs:
            print gatedir

def _safe_mkdir(d):
    try:
        os.mkdir(d)
    except OSError:
        pass

def rpt_getnew(args):
    parser = ArgumentParser(prog='mx rpt-getnew')
    _add_common_args(parser)
    args = parser.parse_args(args)

    if not os.path.exists(args.logdir):
        _safe_mkdir(args.logdir)

    gatedirs = get_gate_dirs(gate_url, _is_result_dir, _strip_dotslash)
    localdirs = get_local_dirs(args.logdir)
    for gatedir in gatedirs:
        gate_url_dir = join(gate_url, gatedir)
        local_dir = join(args.logdir, gatedir)
        if not gatedir in localdirs:
            if args.verbose:
                print 'processing: ' + gatedir
            f = urllib.urlopen(join(gate_url_dir, 'testlog'))
            testlog = f.read()
            _safe_mkdir(local_dir)
            with open(join(local_dir, 'testlog'), 'w') as t:
                t.write(testlog)
#        if True:
            # get test results
            gate_url_test = join(gate_url_dir, 'test')
            local_dir_test = join(local_dir, 'test')
            _safe_mkdir(local_dir_test)
            pkgs = get_gate_dirs(gate_url_test, _is_package_dir, _strip_slash)
            for pkg in pkgs:
                if args.verbose:
                    print '  processing package: ' + pkg
                gate_url_test_pkg = join(gate_url_test, pkg)
                local_dir_test_pkg = join(local_dir_test, pkg)
                _copy_files(gate_url_test_pkg, local_dir_test_pkg, pkg)

def _copy_files(url, local, pkg):
    _safe_mkdir(local)
    testfiles = get_gate_dirs(url, _is_package_dir)
    testsdir = pkg + '-tests'
    for testfile in testfiles:
        if _strip_slash(testfile) == testsdir:
            _copy_files(join(url, testsdir), join(local, testsdir), pkg)
        elif testfile.endswith('/'):
            # have seen problems with recursive hard links
            continue
        else:
            f = urllib.urlopen(join(url, testfile))
            content = f.read()
            with open(join(local, testfile), 'w') as t:
                t.write(content)

def rpt_install_summary(args):
    parser = ArgumentParser(prog='mx rpt-install-summary')
    _add_common_args(parser)
    args = parser.parse_args(args)

    pkgtable = _build_pkgtable(_get_results(args.logdir))
    ok = 0
    failed = 0
    for _, occurrences in pkgtable.iteritems():
        if occurrences[0].status == "OK":
            ok = ok + 1
        elif occurrences[0].status == "FAILED":
            failed = failed + 1
    print "package installs: " + str(len(pkgtable)) + ", ok: " + str(ok) + ", failed: " + str(failed)

def rpt_findmatches(args):
    parser = ArgumentParser(prog='mx rpt-findmatches')
    parser.add_argument("--print-matches", action='store_true', help='print matching lines in find-matches')
    parser.add_argument("--list-file-matches", action='store_true', help='show files in find-matches')
    parser.add_argument('--match-class', action='store', help='override default MatchClass')
    _add_common_args(parser)
    args = parser.parse_args(args)

    results = _get_results(args.logdir)
    find_matches(results, args.find_matches, args.print_matches, args.list_file_matches, args.match_class)

def rpt_install_status(args):
    parser = ArgumentParser(prog='mx rpt-install-status')
    parser.add_argument('--detail', action='store_true', help='display package status')
    parser.add_argument('--displaymode', action='store', default='latest', help='display mode: all | latest')
    parser.add_argument('--failed', action='store_true', help='list packages that failed to install')
    _add_pattern_arg(parser)
    _add_common_args(parser)
    args = parser.parse_args(args)

    pkgtable = _build_pkgtable(_get_results(args.logdir))

    if args.detail:
        for pkgname, occurrences in pkgtable.iteritems():
            if re.search(args.pattern, pkgname) is None:
                continue
            print pkgname
            if args.displaymode == 'all':
                for occurrence in occurrences:
                    print occurrence
            else:
                print occurrences[0]

    pkgnames = []
    for pkgname, occurrences in pkgtable.iteritems():
        if re.search(args.pattern, pkgname) is None:
            continue
        status = occurrences[0].status
        if args.failed:
            if status == "FAILED":
                pkgnames.append(pkgname)
        else:
            if status == "OK":
                pkgnames.append(pkgname)
    pkgnames.sort()
    for pkgname in pkgnames:
        print pkgname

def rpt_test_status(args):
    parser = ArgumentParser(prog='mx rpt-test-status')
    _add_common_args(parser)
    _add_pattern_arg(parser)
    parser.add_argument('--all', action='store_true', help='shows status for all runs')
    args = parser.parse_args(args)

    gnur = _gather_test_outputs(join(os.getcwd(), "test_gnur"))
    fastr_resultInfo_map = _gather_all_test_outputs(args.logdir, args.all)
    for pkg, resultInfo_list in fastr_resultInfo_map.iteritems():
        if not re.search(args.pattern, pkg):
            continue
        testcount = len(resultInfo_list)
        for index in range(testcount):
            fastr = _get_test_outputs(fastr_resultInfo_map, index, pkg)
            rdict = _rpt_compare_pkgs(gnur, fastr, args.verbose, args.pattern)
            if rdict.has_key(pkg):
                # missing if test_gnur missing results for this package
                rc = rdict[pkg]
                print pkg + ' (' + str(fastr_resultInfo_map[pkg][index].date) + ')' + ': ' + ('OK' if rc == 0 else 'FAILED')
            if not args.all:
                break

def rpt_test_summary(args):
    parser = ArgumentParser(prog='mx rpt-test-summary')
    _add_common_args(parser)
    _add_pattern_arg(parser)
    args = parser.parse_args(args)

    fastr_resultInfo_map = _gather_all_test_outputs(args.logdir)
    fastr = _get_test_outputs(fastr_resultInfo_map)
    gnur = _gather_test_outputs(join(os.getcwd(), "test_gnur"))
    rdict = _rpt_compare_pkgs(gnur, fastr, args.verbose, args.pattern)
    ok = 0
    failed = 0
    for _, rc in rdict.iteritems():
        if rc == 0:
            ok = ok + 1
        else:
            failed = failed + 1
    print "package tests: " + str(len(rdict)) + ", ok: " + str(ok) + ", failed: " + str(failed)

class SymbolClassMatch(MatchClass):
    def __init__(self, print_matches, list_file_matches, match_string):
        MatchClass.__init__(self, print_matches, list_file_matches, match_string)
        self.symlist = dict()

    def record(self, pkgname, line, filename):
        lx = line.rfind(':')
        sym = line[lx + 2:]
        self.symlist[sym] = sym

    def output(self):
        for sym in sorted(self.symlist):
            self.findit(sym)

    def findit(self, sym):
        try:
            subprocess.check_output('fgrep ' + sym +' com.oracle.truffle.r.native/fficall/jni/src/*.c', shell=True)
        except subprocess.CalledProcessError:
            print sym

_commands = {
    'rpt-listnew' : [rpt_listnew, '[options]'],
    'rpt-getnew' : [rpt_getnew, '[options]'],
    'rpt-install-summary' : [rpt_install_summary, '[options]'],
    'rpt-test-summary' : [rpt_test_summary, '[options]'],
    'rpt-findmatches' : [rpt_findmatches, '[options]'],
    'rpt-install-status' : [rpt_install_status, '[options]'],
    'rpt-test-status' : [rpt_test_status, '[options]'],
    'rpt-compare': [rpt_compare, '[options]'],
    'rpt-check-install-log': [rpt_check_install_log, '[options]'],
    'rpt-list-testdates' : [rpt_list_testdates, '[options]'],
    'pkgtestanalyze': [rpt_compare, '[options]'],
}
