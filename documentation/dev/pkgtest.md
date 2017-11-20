# PACKAGE TEST DOCUMENTATION

## INSTALLED PACKAGES CACHE

### Description

Avoids re-installing of packages for every test. Packages are cached for a specific native API version, i.e., checksum of the native header files.

Directory structure:
- pkg-cache-dir
--+- version.table
--+- libraryVERSION0
----+- pkgdir0
----+- pkgdir1
----+- ...
--+- libraryVERSION1
----+- pkgdir0
----+- pkgdir1
----+- ...
--+- ...

The API checksum must be provided because we do not want to rely on some R package to compute it.

### Usage

Run `mx pkgtest --cache-pkgs version=<checksum>,dir=<pkg-cache-dir>,size=<cache-size>`, e.g.
```
mx pkgtest --cache-pkgs version=730e109bd7a8a32b1cb9d9a09aa2325d2430587ddbc0c38bad911525,dir=/tmp/cache_dir
```

The `version` key specifies the API version to use, i.e., a checksum of the header files of the native API (mandatory, no default).  
The `pkg-cache-dir` key specifies the directory of the cache (mandatory, no default).  
The `size` key specifies the number of different API versions for which to cache packages (optional, default=`2L`).  

### Details

The version must be provided externally such that the R script does not rely on any package.
The version must reflect the native API in the sense that if two R runtimes have the same native API version, then the packages can be used for both runtimes.

