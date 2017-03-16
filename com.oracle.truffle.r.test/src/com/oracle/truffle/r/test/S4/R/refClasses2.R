MatrixClass = setRefClass(
Class = "MatrixClass",
fields = list(
dataMat = "matrix",
detMat ="numeric",
inverseMat = "matrix"))

MatrixClass$methods(
initialize = function(extMat = diag(1), ...){

dataMat <<- extMat

detMat <<- det(dataMat)
if(abs(detMat) > 1e-7 ){
inverseMat <<- solve(dataMat)
}else{
inverseMat <<- NULL
}

callSuper(...)
})

extMat = diag(3) * c(1,2,3)
newMat = MatrixClass$new(extMat)
newMat