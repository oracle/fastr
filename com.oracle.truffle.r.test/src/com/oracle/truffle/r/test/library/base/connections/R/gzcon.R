gzconTestList <- list(1,2,3)
tmpf <- tempfile()
save(gzconTestList, file=tmpf) 
gzconTestList <- NULL
u <- file(tmpf, open="rb")
load(u)
print(gzconTestList)
gzconTestList <- NULL
u <- url(paste0("file:///", tmpf), open="rb")
load(u)
print(gzconTestList)