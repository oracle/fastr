library(rJava)
# restrict memory so we run out of it earlier
.jinit(".","-Xmx128m")
gc()
.s <- as.character(1:10000)
.i <- 1:10000
..s <- .s
..i <- .i
cat(.jcall("Leaks","S","reportMem"),"\n")
cat(" - create unassigned objects\n")
for (i in 1:400) .jnew("Leaks", .i, .s)
cat(.jcall("Leaks","S","reportMem"),"\n")
cat(" running R gc\n")
gc()
cat(" running java GC\n")
cat(.jcall("Leaks","S","reportMem"),"\n")
cat(" - static pass thorugh parameters\n")
for (i in 1:800) {
  if (i==400) { cat('   (forcing R gc)\n'); gc() }
  .i <- .jcall("Leaks", "[I", "passI", .i)
  .s <- .jcall("Leaks", "[S", "passS", .s)
}
cat(.jcall("Leaks","S","reportMem"),"\n")
cat(" running R gc\n")
gc()
cat(" running java GC\n")
.jcall("Leaks","V","runGC")
cat(.jcall("Leaks","S","reportMem"),"\n")
if (!isTRUE(all.equal(.s, ..s)))
  stop("FAILED - string array was modified")
if (!isTRUE(all.equal(.i, ..i)))
  stop("FAILED - integer array was modified")
cat(" - dynamic storage\n")
l <- .jnew("Leaks", .i, .s)
for (i in 1:800) {
  .i <- .jcall("Leaks", "[I", "passI", .i)
  .s <- .jcall("Leaks", "[S", "passS", .s)
}
cat(.jcall("Leaks","S","reportMem"),"\n")
cat(" running R gc\n")
gc()
cat(.jcall("Leaks","S","reportMem"),"\n")
cat(" running java GC\n")
.jcall("Leaks","V","runGC")
cat(.jcall("Leaks","S","reportMem"),"\n")
if (!isTRUE(all.equal(.s, ..s)))
  stop("FAILED - string array was modified")
if (!isTRUE(all.equal(.i, ..i)))
  stop("FAILED - integer array was modified")

cat("OK\n")
