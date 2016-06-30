g.default <- function(y,...) { cat('g.default args:\n'); print(list(if(missing(y)) NULL else y,...)); }
g.c <- function(x,...) { cat('g.c args:\n'); print(list(if(missing(x)) NULL else x,...)); }
g <- function(x,...) { cat('dispatch\n'); UseMethod('g') }
v <- structure(42,class='c');
sd <- function(i,r) { cat('side effect ',i, '\n');r }

g(y=v)
g(x=v)
g(y=v,x=42)

# here we should have hit the cache limit of CallMatcherNode: the following tests the generic call matcher:

g(y=42,x=v)
g(y=v,z=42)

g(y=sd('y',v), z=sd('z',42))
g(z=sd('z',42), y=sd('y',v))
g()