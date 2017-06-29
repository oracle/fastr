# FastR Allocation Profiler - Getting Started 

The basic usage is illustrated by the following examples:

* Activating the profiling instrument
```
.fastr.profmem(TRUE)
```

* Actual profiling 
```
example(glm)
```
* Creating a snapshot
```
s1 <- .fastr.profmem.snapshot("s1")
```
* Displaying the result
```
.fastr.profmem.show(snapshot=s1)
```
producing:
```
/<main>[id=0] { size: 35155856, count: 140515 }
/<main>[id=0]/example[id=2788] { size: 17197400, count: 87422 }
/<main>[id=0]/example[id=2788]/source[id=2987] { size: 16747568, count: 82327 }
/<main>[id=0]/example[id=2788]/source[id=2987]/source[id=3107] { size: 14999312, count: 76365 }
/<main>[id=0]/example[id=2788]/source[id=2987]/source[id=3107]/source[id=3108] { size: 10815880, count: 56889 }
/<main>[id=0]/example[id=2788]/source[id=2987]/source[id=3107]/source[id=3108]/eval[id=3109] { size: 10784176, count: 56823 }
/<main>[id=0]/example[id=2788]/source[id=2987]/source[id=3107]/source[id=3108]/eval[id=3109]/<no source>[id=8056] { size: 4317704, count: 684 }
/<main>[id=0]/example[id=2788]/source[id=2987]/source[id=3107]/source[id=3108]/eval[id=3109]/<no source>[id=8056]/<no source>[id=8058] { size: 4317704, count: 684 }
```

* Displaying the result, 3 top levels only
```
.fastr.profmem.show(3, snapshot=s1)
```
producing:
```
/<main>[id=0] { size: 35140384, count: 140294 }
/<main>[id=0]/example[id=2788] { size: 17197400, count: 87422 }
/<main>[id=0]/example[id=2788]/source[id=2987] { size: 16747568, count: 82327 }
/<main>[id=0]/example[id=2788]/source[id=2803] { size: 292256, count: 3114 }
/<main>[id=0]/example[id=2788]/source[id=2969] { size: 5456, count: 47 }
/<main>[id=0]/example[id=2788]/source[id=2978] { size: 3024, count: 26 }
/<main>[id=0]/example[id=2788]/source[id=2789] { size: 824, count: 15 }
/<main>[id=0]/example[id=2788]/source[id=2796] { size: 112, count: 2 }
/<main>[id=0]/example[id=2788]/source[id=2973] { size: 88, count: 2 }
/<main>[id=0]/example[id=2788]/source[id=2986] { size: 56, count: 1 }
/<main>[id=0]/example[id=2788]/source[id=2795] { size: 32, count: 1 }
/<main>[id=0]/example[id=2788]/source[id=2977] { size: 0, count: 0 }
```
* Showing the source associated with a stack entry
```
# stack entry id = 2630
.fastr.profmem.source(2630, snapshot=s1)
```
producing:
```
<<<  at 53:9
txt <- unlist(x)
>>>  at 53:24
```
* Showing the hot-spots
```
# single-level hospot view
.fastr.profmem.show(2, snapshot=s1, view = "hotspots")
# unlimited hostspot view
.fastr.profmem.show(snapshot=s1, view = "hotspots")
# show the source of the entry 2630 from the hostspot view on the snapshot s1
.fastr.profmem.source(2630, view="hotspots", snapshot=s1)
```
* Deactivating the profilinig instrument
```
.fastr.profmem(FALSE)
```

The `.fastr.profmem.show` builtin offers several arguments for controlling the output:

* `levels`: determines the maximum number of levels displayed
* `desc`: specifies the sorting order, which is TRUE by default, ie. descending
* `id`: displays only the sub tree of the specified stack entry
* `printParents`: forces printing the parent stack entries. It is meaningful in connection with a nun-NULL id parameter value only.