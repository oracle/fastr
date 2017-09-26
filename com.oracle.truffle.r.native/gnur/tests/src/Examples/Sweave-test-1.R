### R code from vignette source '/s/a/k/fastr/library/utils/Sweave/Sweave-test-1.Rnw'

###################################################
### code chunk number 1: Sweave-test-1.Rnw:15-16
###################################################
1:10


###################################################
### code chunk number 2: Sweave-test-1.Rnw:17-18
###################################################
print(1:20)


###################################################
### code chunk number 3: Sweave-test-1.Rnw:22-25
###################################################
1 + 1
1 + pi
sin(pi/2)


###################################################
### code chunk number 4: Sweave-test-1.Rnw:30-34
###################################################
library(stats)
x <- rnorm(20)
print(x)
print(t1 <- t.test(x))


###################################################
### code chunk number 5: Sweave-test-1.Rnw:45-47
###################################################
data(iris)
summary(iris)


###################################################
### code chunk number 6: Sweave-test-1.Rnw:53-55
###################################################
library(graphics)
pairs(iris)


###################################################
### code chunk number 7: Sweave-test-1.Rnw:63-64
###################################################
boxplot(Sepal.Length~Species, data=iris)


