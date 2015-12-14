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
from argparse import ArgumentParser, REMAINDER
from HTMLParser import HTMLParser
from datetime import datetime

def _gather_test_outputs_forpkg(pkgdirpath):
    '''return a list of paths to .Rout/.fail files in pkgdirpath'''
    result = []
    for dirpath, _, files in os.walk(pkgdirpath):
        for f in files:
            if f.endswith(".Rout") or f.endswith(".fail"):
                result.append(join(dirpath, f))
    result.sort()
    return result

def _gather_test_outputs(testdir, pkgs):
    '''return a dict mapping package names to list of output file paths'''
    result = dict()
    for dirpath, dirs, _ in os.walk(testdir):
        for d in dirs:
            if len(pkgs) == 0 or d in pkgs:
                result[d] = _gather_test_outputs_forpkg(join(dirpath, d))
        # only interested in top level
        break
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

def rpt_compare(args):
    '''
    Analyze test package test results by comparing with GnuR output.
    Return 0 if passed, non-zero if failed
    '''
    parser = ArgumentParser(prog='mx rpt_compare')
    parser.add_argument('--fastr-dir', action='store', help='dir containing fastr results', default=os.getcwd())
    parser.add_argument('--pkg', action='store', help='pkg to compare, default all')
    parser.add_argument('--verbose', action='store_true', help='print names of files that differ')
    parser.add_argument('--diff', action='store_true', help='execute given diff program on differing outputs')
    parser.add_argument('--difftool', action='store', help='diff tool', default='diff')
    parser.add_argument('pkgs', nargs=REMAINDER, metavar='pkg1 pkg2 ...')
    args = parser.parse_args(args)

    pkgs = args.pkgs
    if args.pkg:
        pkgs = [args.pkg] + pkgs

    verbose = args.verbose
    gnur = _gather_test_outputs(join(os.getcwd(), "test_gnur"), pkgs)
    if args.pkg:
        if not gnur.has_key(args.pkg):
            mx.abort('no gnur output to compare')

    fastr = _gather_test_outputs(join(args.fastr_dir, "test"), pkgs)
    # gnur is definitive
    result = 0 # optimistic
    for pkg in pkgs:
        if not fastr.has_key(pkg):
            result = 1
            continue

        if not gnur.has_key(pkg):
            print 'no gnur output to compare: ' + pkg

        fastr_outputs = fastr[pkg]
        gnur_outputs = gnur[pkg]
        if len(fastr_outputs) != len(gnur_outputs):
            if verbose:
                print 'fastr is missing some output files'
                # TODO continue but handle missing files in loop?
                # does it ever happen in practice?
            result = 1
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
            result = _fuzzy_compare(gnur_content, fastr_content)
            if result != 0:
                if verbose:
                    print 'mismatch on file: ' + fastr_output
                if args.diff:
                    cmd = [args.difftool, gnur_output, fastr_output]
                    print ' '.join(cmd)
                    subprocess.call(cmd)
                break
    return result

def check_install(result, text):
    lines = text.split("\n")
    nlines = len(lines)
    result_data = ""

    def find_done(index, pkgname):
        for i in range(index, nlines):
            line = lines[i]
            if line.startswith("* DONE"):
                done_pkgname = line[line.find("(") + 1 : line.rfind(")")]
                return pkgname == done_pkgname
        return False

    start_index = None
    for i in range(nlines):
        line = lines[i]
        if line.startswith("BEGIN package installation"):
            start_index = i

    if start_index:
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

def _extract_pkgname(line):
    sx = line.find("'");
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
    def __init__(self, date, cid):
        self.date = datetime.strptime(date, "%Y-%m-%d_%H:%M:%S.%f")
        self.cid = cid

    def __str__(self):
        return "date: {0}, id {1}".format(self.date, self.cid)

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
                j = i;
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
                results.append(Result(ResultInfo(localdir[7:33], localdir[34:74]), result_data, rawData))
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
            occurrences.sort(key = lambda pkgstatus: pkgstatus.resultInfo.date, reverse=True)
    return pkgtable

def _add_common_args(parser):
    parser.add_argument("--logdir", action='store', help='directory of complete log files', default='install.cran.logs')
    parser.add_argument("--verbose", action='store_true', help='verbose output')

def get_gate_dirs(url, matchfun, adjustfun=None):
    gatedirlist = []
    urlf = urllib.urlopen(url)
    text = urlf.read()
    parser = DirHTMLParser(gatedirlist)
    parser.feed(text)
    urlf.close()
    gatedirlist = filter(matchfun, gatedirlist)
    filelist = []
    for gatedir in gatedirlist:
        if adjustfun:
            gatedir = adjustfun(gatedir)
        filelist.append(gatedir)
    return filelist

def get_local_dirs(logdir):
    filelist = []
    for _, localdirs, _ in os.walk(logdir):
        break
    for localdir in localdirs:
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

def rpt_summary(args):
    parser = ArgumentParser(prog='mx rpt-summary')
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
    parser.add_argument('--failed', action='store_true', help='list packages that failed to installed')
    parser.add_argument('pkgs', nargs=REMAINDER, metavar='pkg1 pkg2 ...')
    _add_common_args(parser)
    args = parser.parse_args(args)

    packages = args.pkgs
    pkgtable = _build_pkgtable(_get_results(args.logdir))

    if args.detail:
        for pkgname, occurrences in pkgtable.iteritems():
            if len(packages) == 0 or pkgname in packages:
                print pkgname
                if args.displaymode == 'all':
                    for occurrence in occurrences:
                        print occurrence
                else:
                    print occurrences[0]

    pkgnames = []
    for pkgname, occurrences in pkgtable.iteritems():
        if len(packages) == 0 or pkgname in packages:
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

def rpt_list_testdirs(args):
    parser = ArgumentParser(prog='mx rpt-list-testdirs')
    parser.add_argument('pkg', nargs=REMAINDER)
    _add_common_args(parser)
    args = parser.parse_args(args)

    if len(args.pkg) != 1:
        mx.abort('Exactly one package name is required')

    pkg = args.pkg[0]
    result = []
    local_dirs = get_local_dirs(args.logdir)
    for local_dir in local_dirs:
        testdir = join(args.logdir, local_dir,'test')
        if not os.path.exists(testdir):
            continue
        pkgdirs = os.listdir(testdir)
        if pkg in pkgdirs:
            result.append(testdir)
    for r in result:
        print r

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
            pass
        except subprocess.CalledProcessError:
            print sym

_commands = {
    'rpt-listnew' : [rpt_listnew, '[options]'],
    'rpt-getnew' : [rpt_getnew, '[options]'],
    'rpt-summary' : [rpt_summary, '[options]'],
    'rpt-findmatches' : [rpt_findmatches, '[options]'],
    'rpt-install-status' : [rpt_install_status, '[options]'],
    'rpt-list-testdirs' : [rpt_list_testdirs, '[options]'],
    'rpt-compare': [rpt_compare, '[options]'],
    'pkgtestanalyze': [rpt_compare, '[options]'],
}
