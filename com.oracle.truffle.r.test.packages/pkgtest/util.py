import logging
import argparse
import hashlib
import subprocess
import sys
import os
from os.path import join
from datetime import datetime

_opts = argparse.Namespace()


def get_fastr_home():
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


def get_fastr_include_path():
    if get_graalvm_home():
        return join(_opts.graalvm, "jre", "languages", "R", "include")
    return join(get_fastr_home(), 'include')


def graalvm_rscript():
    assert _opts.graalvm is not None
    return join(_opts.graalvm, 'bin', 'Rscript')


def get_fastr_rscript():
    _opts.graalvm_dir = get_graalvm_home()
    if _opts.graalvm_dir is not None:
        return join(_opts.graalvm_dir, "bin", "Rscript")
    return join(get_fastr_home(), 'bin', 'Rscript')


def get_r_version(rscript_binary):
    args = ["--silent", "-e", "cat(R.Version()[['major']], '.', R.Version()[['minor']], '\\n', sep='')"]
    return subprocess.check_output([rscript_binary] + args, stderr=subprocess.STDOUT).rstrip()


def get_graalvm_home():
    return _opts.graalvm_home

def abort(status, *args):
    if args:
        logging.error(*args)
    quit(status)


def log_step(state, step, rvariant):
    if not _opts.quiet:
        print ("{0} {1} with {2}".format(state, step, rvariant))
        log_timestamp()


def log_timestamp():
    if not _opts.quiet:
        print ("timestamp: {0}".format(str(datetime.now())))


def check_r_versions():
    '''
    Checks that FastR and GnuR have the same version.
    '''
    gnur_version = get_r_version(get_gnur_rscript())
    fastr_version = get_r_version(get_fastr_rscript())
    logging.info("Using FastR version = %s ; GnuR version = %s: " % (fastr_version, gnur_version))
    if gnur_version != fastr_version:
        abort(1, '_opts.graalvm R version does not match gnur suite: %s (GnuR) vs. %s (FastR)' % (gnur_version, fastr_version))


def parse_arguments(argv):
    """
    Parses the given argument vector and stores the values of the arguments known by this script to appropriate globals.
    The unknown arguments are returned for further processing.
    """
    parser = argparse.ArgumentParser(description='FastR package testing.')
    parser.add_argument('--fastr-home', metavar='FASTR_HOME', dest="fastr_home", type=str, default=None,
                        required=True, help='The FastR standalone repo home directory.')
    parser.add_argument('--gnur-home', metavar="GNUR_HOME", dest="gnur_home", default=None, required=True,
                        help='The GnuR home directory.')
    parser.add_argument('--_opts.graalvm-home', metavar="GRAALVM_HOME", dest="graalvm_home", default=None,
                        help='The _opts.graalvm root directory.')
    parser.add_argument('-v', '--verbose', dest="verbose", action="store_const", const=1, default=0,
                        help='Do verbose logging.')
    parser.add_argument('-V', '--very-verbose', dest="verbose", action="store_const", const=2,
                        help='Do verbose logging.')
    parser.add_argument('--dump-preprocessed', dest="dump_preprocessed", action="store_true",
                        help='Dump processed output files where replacement filters have been applied.')
    parser.add_argument('-q', '--quiet', dest="quiet", type=bool, default=False,
                        help='Do verbose logging.')
    parser.add_argument('-l', '--log-file', dest="log_file", default="pkgtest.log",
                        help='Log file name.')
    global _opts
    _opts, r_args = parser.parse_known_args(args=argv)


    if _opts.verbose == 1:
        log_level = logging.INFO
    elif _opts.verbose == 2:
        log_level = logging.DEBUG
    logging.basicConfig(filename=_opts.log_file, level=log_level)

    # also log to console
    logging.getLogger("").addHandler(logging.StreamHandler(stream=sys.stdout))

    logging.debug("known_args: %s" % _opts)

    # print info if _opts.graalvm is used
    if get_graalvm_home():
        logging.info("Using _opts.graalvm at %r" % get_graalvm_home())

    # ensure that FastR and GnuR have the same version
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