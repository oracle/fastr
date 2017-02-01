Objects in FastR can be in three states: _temporary_, _non-shared_ and _shared_, this is handled through `RShareable` interface. Once an object is written into a frame (a variable) its state is transitioned from _temporary_ to _non-shared_ or from _non-shared_ to _shared_, this transition does not take place if LSH == RHS, e.g. `a <- a`. If some code is about to alter a _shared_ object, it must make a copy. _Temporary_ objects can be altered freely. _Non-shared_ objects can be altered in place in replacement functions, but otherwise should be copied as well. With this schema we can implement value semantics and avoid some expensive copying.

Summary of the states: Temporary -> Non shared (at most one reference) -> Shared (may be referenced 2 or more places)

### Reference counting for parameters

TODO: to be documented.

### Replacement functions

following code:

`a[1] <- 42`

is rewritten by the parser to the following sequence:

```r
a <- `[<-`(a, 1, 42)
```

The replacement function can alter "`a"` as long as it is _non-shared_, then it returns the same instance and the write into the frame concludes LHS == RHS and does no state transition. If "a" is _non-shared_, but cannot be used as is, e.g. because we need to cast it to another type, the replacement returns fresh instance in _temporary_ state which will be transitioned to _non-shared_ by the write into the frame, because in that case LHS != RHS.

### Complex data structures: lists and attributes

Lists can contain other objects – like frame contains local variables, object in a list can be referenced through the list (like with local variable name). Moreover, because lists have value semantics (unlike environments), once we pass a list to a function as an argument "a" any changes to values inside "a", e.g. "a$f[[1]] <- 42" should not be visible from the outside. Example:

```r
x <- list(f = c(1,2,3))  
foo <- function(a) a$f[[1]] <- 42  
foo(x)  
cat(x$f[[1]]) # should print 1, not 42
```

If we wanted to have consistent reference count for objects contained within lists, we would have to increment reference count of "x" and all items (recursively) inside x, which can be costly operation.

Another problematic part are replacements where lists are involved:

```r
k$bar[1] <- 42
```

is rewritten as

```r
tmp <- `$`(k, 'bar')  
tmp[1] <- 42  
k <- `$<-`(tmp, 'bar', 42)
```

If "k" is _non-shared_ and there is some other vector in k$foo which is also non-shared, we want it to stay non-shared after this operation to avoid possible future copying. Imagine that "k" does not yet have a field named "bar", this means that "k" has to be reallocated and the replacement will return different instance of RList (but the original RList will be lost after the replacement). If the reference counting is not done in smart way, "k$foo" will be marked as shared (referenced by list that is now in "k" and by the list we thrown away because original "k" needed to be reallocated).

Note: everything in this section holds for attributes as well.

### Deferred ref-count increment for list elements and attributes:

Lists can hold elements that are not in a consistent state with respect to the sharing model. However, such elements are put into consistent state once they are read from the list, their state can be inferred from the state of the owning list:

*   owner is shared -> element must be shared
*   owner is non-shared -> element must be at least non-shared
*   owner is temporary -> leave the element as is (can be even temporary?)

Important observation here is that there is no possible way, how a list can contain a non-shared element not owned by it, given that any element of some other list must be first read and the extraction from list makes it at least non-shared and only then it can be put inside another list. On a write to a list, we do increment ref-count, so it will become shared.

This means that <u>temporary</u> elements can be freely put into lists without incrementing their ref-count. Moreover, when a list is copied into another variable or passed as an argument to a function, we only have to increment the ref-count of the list (no recursion needed). This is how most of the code works now (e.g. putting temps to lists happens often in built-ins).

The catch: now considering the internal Java code of buitins: reads and writes from a list should go through special nodes, unless they have certain properties: write of temporary to a list, or read only to peek at the element (e.g. calculate something from it) and then forget the reference (or at least not reveal it to the R user in an environment, attribute, or list), otherwise `ExtractListElement` node should be used for reads. In the case of writes: I have not found yet a code that would put potentially non-temporary data into a list.

It seems that most of the internal code that works with lists actually has these properties.

### Unresolved problem: cycles

`l <- list(list()); l[[1]] <- l; l`, this currently crashes FastR, but GnuR can handle it, because they call `Fixup_RHS` before each assignment operation and this function traverses RHS to check if it contains reference to LSH (if so, makes a copy). This would mean a very costly list traversal on each assignment.

We can remember for each list, if it is recursive, like we remember for vectors, if they contain `NA`. `RDataFactory.createList` would take `boolean isRecursive` (it is often known that newly created list only contains non-lists) and every update of a list would have to update that property accordingly.
