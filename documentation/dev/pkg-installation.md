# Package installation
## Description
Notes on packages installation: R CMD INSTALL path/to/package

The R bash script delegates to $R_HOME/bin/Rcmd,
which delegates to $R_HOME/bin/INSTALL, where we have

echo 'tools:::.install_packages()' | R_DEFAULT_PACKAGES= LC_COLLATE=C "${R_HOME}/bin/R" $myArgs --no-eco --args ${args}

which is, for example, for rlang:

R_DEFAULT_PACKAGES= LC_COLLATE=C $R_HOME/bin/R --no-restore --no-echo --args nextArg../../../local/pkgsrc/rlang

So the main routine of package installation is tools:::.install_packages, 
which reads the R level command line argument "nextArg". You can also directly invoke it:

tools:::.install_packages('/path/to/package')

## Debugging
This section provides some possibilities to debug package installation process.
These possibilities apply both for GNU-R and FastR, if not noted otherwise.

Normally, the installation process of a package invokes child R processes multiple times.
Therefore, attaching java debugger to the first process starting the installation is not very practical.
Instead, set the `MX_R_GLOBAL_ARGS` environment variable to `MX_R_GLOBAL_ARGS=-d` so that each spawned FastR process will start with `-d` option.
Note that `MX_R_GLOBAL_ARGS` environment variable works only when FastR is invoked via `mx`.

Another option is to directly hack `$GNUR_HOME_BINARY/src/library/tools/R/install.R` sources and add, e.g., logging outputs.
Note that for this to work, you have to rebuild FastR.