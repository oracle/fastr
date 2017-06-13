## signed vs unsigned ints
len <- 100000
zz <- rawConnection(raw(0), "wb")
x <- as.raw((1:len) %% 255)
writeBin(x, zz)
res <- rawConnectionValue(zz)
close(zz)

zz <- rawConnection(res, "rb")
res1 <- readBin(zz, "raw", len)
close(zz)
Reduce(`&&`, res == res1, TRUE)