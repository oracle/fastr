stopifnot(require(methods))
stopifnot(require(tests4))

new("numeric")
setClass("foo", representation(j="numeric")); new("foo", j=42)
setClass("foo", representation(j="numeric")); typeof(new("foo", j=42))

setClass("foo", representation(j="numeric")); getClass("foo")

setClass("foo"); setClass("bar", representation(j = "numeric"), contains = "foo"); is.null(getClass("foo")@prototype)
