#
# Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import logging
import argparse
import hashlib
import subprocess
import sys
import os
import tempfile
from os.path import join
from datetime import datetime

fastr_default_testdir = 'test.fastr'
gnur_default_testdir = 'test.gnur'

_opts = argparse.Namespace()


def get_opts():
    return _opts


def get_fastr_repo_dir():
    return _opts.fastr_home


def get_gnur_home():
    '''
    Returns path to GnuR home dir, e.g., gnur/gnur/R-3.4.0/.
    '''
    return _opts.gnur_home


def get_gnur_rscript():
    '''
    returns path to Rscript in sibling gnur directory
    '''
    # return _mx_gnur().extensions._gnur_rscript_path()
    return join(get_gnur_home(), "bin", "Rscript")


def get_gnur_include_path():
    # if graalvm():
    #     return join(_mx_gnur().dir, 'gnur', _mx_gnur().extensions.r_version(), 'include')
    # return join(mx_fastr._gnur_path(), "include")
    return join(get_gnur_home(), 'include')


def get_fastr_home():
    if get_graalvm_home():
        homes = [join(get_graalvm_home(), "jre", "languages", "R"), join(get_graalvm_home(), "languages", "R")]
        for result in homes:
            if os.path.exists(result):
                return result
        logging.error("Could not find FastR home. Tried: " + ",".join(homes))
    return get_fastr_repo_dir()


def get_fastr_include_path():
    return join(get_fastr_home(), 'include')


def graalvm_rscript():
    assert get_graalvm_home() is not None
    return join(get_graalvm_home(), 'bin', 'Rscript')


def get_fastr_rscript():
    graalvm_dir = get_graalvm_home()
    if graalvm_dir is not None:
        return join(graalvm_dir, "bin", "Rscript")
    return join(get_fastr_repo_dir(), 'bin', 'Rscript')


def get_default_cran_mirror():
    default_cran_mirror_file = join(get_fastr_home(), 'etc', 'DEFAULT_CRAN_MIRROR')
    url = None
    try:
        f = open(default_cran_mirror_file, "r")
        url = f.readline()
        if url:
            url = url.strip()
        if 'FASTR_MRAN_MIRROR' in os.environ:
            overlay = os.environ['FASTR_MRAN_MIRROR'].strip()
            overlay += '' if overlay.endswith('/') else '/'
            date = url[-len('2000-00-00'):]
            url = overlay + 'snapshot/' + date
            logging.info("Using '{0}' instead of the default MRAN mirror.".format(url))
    except IOError as e:
        logging.error("Could not read %s: %s" % (default_cran_mirror_file, e))
    finally:
        f.close()
    return url


def get_r_version(rscript_binary, tmp_dir):
    r_version_file = join(tmp_dir, "R.version")
    args = [rscript_binary, "--verbose", "-e", "cat(R.Version()[['major']], '.', R.Version()[['minor']], file='%s', sep='')" % r_version_file]
    if not os.path.exists(rscript_binary):
        abort(1, "Rscript binary '%s' does not exist.", rscript_binary)
    try:
        logging.debug("Running command: %s", args)
        output = subprocess.check_output(args, stderr=subprocess.STDOUT, universal_newlines=True)

        lines = None
        with open(r_version_file, "r") as f:
           lines = f.readlines()
        return lines[-1].strip()
    finally:
        os.unlink(r_version_file)


def get_graalvm_home():
    return _opts.graalvm_home

def abort(status, *args):
    if args:
        logging.fatal(*args)
    quit(status)


def log_step(state, step, rvariant):
    if not _opts.quiet:
        logging.info("{0} {1} with {2}".format(state, step, rvariant))
        log_timestamp()


def log_timestamp():
    if not _opts.quiet:
        logging.info("timestamp: {0}".format(str(datetime.now())))


def check_r_versions():
    '''
    Checks that FastR and GnuR have the same version.
    '''
    tmp_dir = tempfile.mkdtemp()
    gnur_version = get_r_version(get_gnur_rscript(), tmp_dir)
    fastr_version = get_r_version(get_fastr_rscript(), tmp_dir)
    logging.info("Using FastR version = %r ; GnuR version = %r: " % (fastr_version, gnur_version))
    if gnur_version != fastr_version:
        abort(1, 'GraalVM R version does not match GnuR version: %r (FastR) vs. %r (GnuR)' % (fastr_version, gnur_version))


VERY_VERBOSE = logging.DEBUG - 5

def parse_arguments(argv, r_version_check=True):
    """
    Parses the given argument vector and stores the values of the arguments known by this script to appropriate globals.
    The unknown arguments are returned for further processing.
    """
    parser = argparse.ArgumentParser(prog="pkgtest", description='FastR package testing.')
    parser.add_argument('--fastr-home', metavar='FASTR_HOME', dest="fastr_home", type=str, default=None,
                        required=True, help='The FastR standalone repo home directory (required).')
    parser.add_argument('--gnur-home', metavar="GNUR_HOME", dest="gnur_home", default=None, required=True,
                        help='The GnuR home directory (required).')
    parser.add_argument('--graalvm-home', metavar="GRAALVM_HOME", dest="graalvm_home", default=None,
                        help='The GraalVM root directory (optional).')
    parser.add_argument('-v', '--verbose', dest="verbose", action="store_const", const=1, default=0,
                        help='Do verbose logging.')
    parser.add_argument('-V', '--very-verbose', dest="verbose", action="store_const", const=2,
                        help='Do verbose logging.')
    parser.add_argument('--dump-preprocessed', dest="dump_preprocessed", action="store_true",
                        help='Dump processed output files where replacement filters have been applied.')
    parser.add_argument('-q', '--quiet', dest="quiet", action="store_true",
                        help='Do verbose logging.')
    parser.add_argument('--fastr-testdir', metavar="FASTR_TESTDIR", dest="fastr_testdir", default=fastr_default_testdir,
                        help='FastR test result directory (default: "test.fastr").')
    parser.add_argument('--gnur-testdir', metavar="GNUR_TESTDIR", dest="gnur_testdir", default=gnur_default_testdir,
                        help='GnuR test result directory (default: "test.gnur").')
    parser.add_argument('-l', '--log-file', dest="log_file", default="pkgtest.log",
                        help='Log file name (default: "FASTR_TESTDIR/pkgtest.log").')

    global _opts
    _opts, r_args = parser.parse_known_args(args=argv)

    # ensure that first arg is neither the name of this package nor a py file
    if r_args and (r_args[0] == __package__ or r_args[0].endswith(".py")):
        r_args = r_args[1:]

    log_format = '%(message)s'
    if _opts.verbose == 1:
        log_level = logging.DEBUG
    elif _opts.verbose == 2:
        log_level = VERY_VERBOSE
    else:
        log_level = logging.INFO
    logging.basicConfig(filename=_opts.log_file, filemode="w", level=log_level, format=log_format)

    # also log to console
    console_handler = logging.StreamHandler(stream=sys.stdout)
    console_handler.setLevel(log_level)

    console_handler.setFormatter(logging.Formatter(log_format))
    logging.getLogger("").addHandler(console_handler)

    logging.log(VERY_VERBOSE, "known_args: %s" % _opts)

    # print info if _opts.graalvm is used
    if get_graalvm_home():
        logging.info("Using GraalVM at %r" % get_graalvm_home())

    # ensure that FastR and GnuR have the same version
    if r_version_check:
        check_r_versions()

    return r_args


def computeApiChecksum(includeDir):
    """
    Computes a checksum of the header files found in the provided directory (recursively).
    The result is a SHA256 checksum (as string with hex digits) of all header files.
    """
    m = hashlib.sha256()
    rootDir = includeDir
    fileList = list()
    for root, _, files in os.walk(rootDir):
        logging.log(VERY_VERBOSE, "Visiting directory %s" % root)
        for f in files:
            fileName = join(root, f)
            if fileName.endswith('.h'):
                logging.log(VERY_VERBOSE, "Including file %s" % fileName)
                fileList.append(fileName)

    # sorting makes the checksum independent of the FS traversal order
    fileList.sort()
    for fileName in fileList:
        try:
            with open(fileName) as f:
                m.update(f.read().encode())
        except IOError as e:
            # Ignore errors on broken symlinks
            if not os.path.islink(fileName) or os.path.exists(fileName):
                raise e

    hxdigest = m.hexdigest()
    logging.debug("Computed API version checksum {0}".format(hxdigest))
    return hxdigest


def ensure_dir(path):
    if not os.path.exists(path):
        os.makedirs(path)
    return path