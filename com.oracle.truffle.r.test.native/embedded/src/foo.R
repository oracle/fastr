# Note: taken from GNU-R sources tests/Embedding
foo <-
function(...)
{
    args <- list(...)
    print(args)
    print(names(args))
    TRUE
}
