Notes on packages installation: R CMD INSTALL path/to/package

The R bash script delegates to $R_HOME/bin/Rcmd,
which delegates to $R_HOME/bin/INSTALL, where we have

echo 'tools:::.install_packages()' | R_DEFAULT_PACKAGES= LC_COLLATE=C "${R_HOME}/bin/R" $myArgs --no-eco --args ${args}

which is, for example, for rlang:

R_DEFAULT_PACKAGES= LC_COLLATE=C $R_HOME/bin/R --no-restore --no-echo --args nextArg../../../local/pkgsrc/rlang

So the main routine of package installation is tools:::.install_packages, 
which reads the R level command line argument "nextArg". You can also directly invoke it:

tools:::.install_packages('/path/to/package')
