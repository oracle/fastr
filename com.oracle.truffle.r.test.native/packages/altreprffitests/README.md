# `altreprffitests` package
This is a package for testing various features of the *altrep* framework.
It contains as little native code as possible and tests some high-level features
of altrep framework.

## How it works
This package provides `vec_wrapper` altrep class that represents very simple
wrapper for whatever data it gets into its "constructor" `vec_wrapper.create_instance`.
Moreover, one can specify which altrep methods should be overriden with
`gen.*` parameters to the constructor.


## See also
Note that there also exist a different package for testing altrep instances
called `altrep-class-tests`.
The main difference between the `altrep-class-tests` package and this package
is that this package is able to test more altrep instances at the same time,
while the `altrep-class-tests` package tests just one altrep instance.
