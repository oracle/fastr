#
# This material is distributed under the GNU General Public License
# Version 2. You may review the terms of this license at
# http://www.gnu.org/licenses/gpl-2.0.html
#
# Copyright (c) 1995-2014, The R Core Team
# Copyright (c) 2016, Oracle and/or its affiliates
#
# All rights reserved.
#

eval(expression({
.libPaths <- local({
    .lib.loc <- character()            # Profiles need to set this.
    function(new) {
        if(!missing(new)) {
            ## paths don't really need to be unique, but searching
            ## large library trees repeatedly would be inefficient.
            ## Use normalizePath for display: but also does path.expand
            new <- Sys.glob(path.expand(new))
            paths <- unique(normalizePath(c(new, .Library.site, .Library), '/'))
            .lib.loc <<- paths[dir.exists(paths)]
            .fastr.libPaths(.lib.loc)
        }
        else
            .lib.loc
    }
})
}), asNamespace("base"))

makeActiveBinding(".Random.seed", .fastr.set.seed, .GlobalEnv)
