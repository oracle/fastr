#### Run all demos for which we do not wish to diff the output
.ptime <- proc.time()
set.seed(123)

# FastR changed: demos <- c("Hershey", "Japanese", "lm.glm", "nlm", "plotmath")
demos <- c()

for(nam in  demos) demo(nam, character.only = TRUE)

# FastR commented-out: cat("Time elapsed: ", proc.time() - .ptime, "\n")
