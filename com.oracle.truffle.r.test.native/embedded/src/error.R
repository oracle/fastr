# Note: taken from GNU-R sources tests/Embedding
foo <-
function()
{
    on.exit(print(1:10))
    stop("Stopping in function foo")
}
