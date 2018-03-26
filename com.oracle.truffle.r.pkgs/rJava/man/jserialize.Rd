\name{jserialize}
\alias{.jserialize}
\alias{.junserialize}
\alias{.jcache}
\title{
  Java object serialization
}
\description{
  \code{.jserialize} serializes a Java object into raw vector using
  Java serialization.

  \code{.junserialize} re-constructs a Java object from its serialized
  (raw-vector) form.

  \code{.jcache} updates, retrieves or removes R-side object cache
  which can be used for persistent storage of Java objects across
  sessions.
}
\usage{
.jserialize(o)
.junserialize(data)
.jcache(o, update=TRUE)
}
\arguments{
  \item{o}{Java object}
  \item{data}{serialized Java object as a raw vector}
  \item{update}{must be \code{TRUE} (cache is updated), \code{FALSE}
  (cache is retrieved) or \code{NULL} (cache is deleted).}
}
\value{
  \code{.jserialize} returns a raw vector

  \code{.junserialize} returns a Java object or \code{NULL} if an error
  occurred (currently you may use \code{.jcheck()} to further
  investigate the error)

  \code{.jcache} returns the current cache (usually a raw vector) or
  \code{NULL} if there is no cache.
}
\details{
  Not all Java objects support serialization, see Java documentation
  for details. Note that Java serialization and serialization of R
  objects are two entirely different mechanisms that cannot be
  interchanged. \code{.jserialize} and \code{.junserialize} can
  be used to access Java serialization facilities.

  \code{.jcache} manipulates the R-side Java object cache associated
  with a given Java reference:

  Java objects do not persist across sessions, because the Java
  Virtual Machine (JVM) is destroyed when R is closed. All saved Java
  object references will be restored as \code{null} references, since
  the corresponding objects no longer exist (see R documentation on
  serialization). However, it is possible to serialize a Java object
  (if supported by the object) and store its serialized form in
  R. This allows for the object to be deserialized when loaded into
  another active session (but see notes below!)

  R-side cache consists of a serialized form of the object as raw
  vector. This cache is attached to the Java object and thus will be
  saved when the Java object is saved. rJava provides an automated way
  of deserializing Java references if they are \code{null} references
  and have a cache attached. This is done on-demand basis whenever a
  reference to a Java object is required.

  Therefore packages can use \code{.jcache} to provide a way of
  creating Java references that persist across sessions. However, they
  must be very cautious in doing so. First, make sure the serialized
  form is not too big. Storing whole datasets in Java serialized form
  will hog immense amounts of memory on the R side and should be
  avoided. In addition, be aware that the cache is just a snapshot, it
  doesn't change when the referenced Java object is modified. Hence it
  is most useful only for references that are not modified outside
  R. Finally, internal references to other Java objects accessible
  from R are not retained (see below). Most common use of
  \code{.jcache} is with Java references that point to definitions of
  methods (e.g., models) and other descriptive objects which are then
  used by other, active Java classes to act upon. Caching of such
  active objects is not a good idea, they should be instantiated by
  functions that operate on the descriptive references instead.

  \emph{Important note:} the serialization of Java references does NOT
  take into account any dependencies on the R side. Therefore if you
  hold a reference to a Java object in R that is also referenced by
  the serialized Java object on the Java side, then this relationship
  cannot be retained upon restore. Instead, two copies of disjoint
  objects will be created which can cause confusion and errorneous
  behavior.

  The cache is attached to the reference external pointer and thus it
  is shared with all copies of the same reference (even when changed
  via \code{\link{.jcast}} etc.), but it is independent of other
  references to the object obtained separately
  (e.g., via \code{\link{.jcall}} or \code{\link{.jfield}}).

  Also note that deserialization (even automated one) requires a
  running virtual machine. Therefore you must make sure that either
  \code{\link{.jinit}} or \code{\link{.jpackage}} is used before any
  Java references are accessed.
}
%\seealso{
%}
%\examples{
%\dontrun{
%}
%}
\keyword{interface}
